package com.hubinity.catalog.integration;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.hubinity.catalog.config.RabbitConfig;
import com.hubinity.catalog.domain.OutboxMessage;
import com.hubinity.catalog.domain.OutboxMessageRepository;

import io.micrometer.tracing.Tracer;

/**
 * Lê lotes de mensagens PENDING do outbox e publica no exchange {@code catalog.events}.
 *
 * <h3>Garantias de concorrência</h3>
 * Cada batch é processado dentro de uma TX curta via {@link TransactionTemplate}
 * (não via {@code @Transactional} no método agendado — o lock SKIP LOCKED deve
 * durar apenas o tempo de um batch, não de todo o job). Dois dispatchers concorrentes
 * adquirem conjuntos disjuntos de locks e nunca processam a mesma mensagem.
 *
 * <h3>Retry e DLQ</h3>
 * Falhas no {@code send()} incrementam {@code attempts}. Após {@code maxAttempts}
 * falhas, a mensagem é marcada {@code FAILED} e publicada explicitamente no DLX
 * (fanout → DLQ) para inspeção operacional.
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxMessageRepository outboxMessages;
    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;
    private final TransactionTemplate txTemplate;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxDispatcher(
            OutboxMessageRepository outboxMessages,
            RabbitTemplate rabbitTemplate,
            PlatformTransactionManager txManager,
            ObjectProvider<Tracer> tracerProvider,
            @Value("${app.outbox.dispatch.batch-size:50}") int batchSize,
            @Value("${app.outbox.dispatch.max-attempts:5}") int maxAttempts) {
        this.outboxMessages = outboxMessages;
        this.rabbitTemplate = rabbitTemplate;
        this.tracer = tracerProvider.getIfAvailable();
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /**
     * Dispara em loop, cada iteração processando um batch em TX própria.
     * Para quando o batch retorna menos que {@code batchSize} (outbox vazio ou quase).
     */
    @Scheduled(fixedDelayString = "${app.outbox.dispatch.delay-ms:5000}")
    public void dispatchPending() {
        int dispatched;
        do {
            dispatched = processOneBatch();
        } while (dispatched == batchSize);
    }

    /**
     * TX curta: adquire SKIP LOCKED, publica cada mensagem, atualiza status, commita.
     * Os row-locks são liberados ao final desta chamada, não do job inteiro.
     */
    int processOneBatch() {
        Integer count = txTemplate.execute(status -> {
            List<OutboxMessage> batch = outboxMessages.findBatchForDispatch(batchSize);
            for (OutboxMessage msg : batch) {
                dispatch(msg);
            }
            return batch.size();
        });
        return count != null ? count : 0;
    }

    private void dispatch(OutboxMessage msg) {
        try {
            MessageProperties props = new MessageProperties();
            props.setHeader("messageId", msg.getMessageId().toString());
            props.setHeader("eventType", msg.getEventType());
            props.setHeader("schemaVersion", msg.getSchemaVersion());

            // traceparent do span atual; null é aceitável em contexto agendado sem trace ativo
            String traceparent = currentTraceparent();
            if (traceparent != null) {
                props.setHeader("traceparent", traceparent);
            }

            byte[] body = msg.getPayload().getBytes(StandardCharsets.UTF_8);
            rabbitTemplate.send(RabbitConfig.EXCHANGE, msg.getRoutingKey(), new Message(body, props));

            msg.markPublished(Instant.now());
            log.debug("event=outbox_published messageId={} routingKey={}",
                    msg.getMessageId(), msg.getRoutingKey());

        } catch (Exception e) {
            msg.recordFailure(e.getMessage());
            log.warn("event=outbox_dispatch_failed messageId={} attempts={} error={}",
                    msg.getMessageId(), msg.getAttempts(), e.getMessage());

            if (msg.getAttempts() >= maxAttempts) {
                msg.markFailed();
                sendToDlq(msg);
                log.error("event=outbox_failed messageId={} routingKey={} attempts={}",
                        msg.getMessageId(), msg.getRoutingKey(), msg.getAttempts());
            }
        }
        outboxMessages.save(msg);
    }

    private void sendToDlq(OutboxMessage msg) {
        try {
            MessageProperties props = new MessageProperties();
            props.setHeader("messageId", msg.getMessageId().toString());
            props.setHeader("eventType", msg.getEventType());
            props.setHeader("x-original-routing-key", msg.getRoutingKey());
            if (msg.getLastError() != null) {
                props.setHeader("x-last-error", msg.getLastError());
            }
            byte[] body = msg.getPayload().getBytes(StandardCharsets.UTF_8);
            // DLX é fanout — routing key vazia, a binding encaminha para o DLQ
            rabbitTemplate.send(RabbitConfig.DLX, "", new Message(body, props));
        } catch (Exception dlqEx) {
            log.error("event=dlq_publish_failed messageId={} error={}",
                    msg.getMessageId(), dlqEx.getMessage());
        }
    }

    private String currentTraceparent() {
        if (tracer == null) return null;
        var span = tracer.currentSpan();
        if (span == null) return null;
        var ctx = span.context();
        return "00-%s-%s-01".formatted(ctx.traceId(), ctx.spanId());
    }
}

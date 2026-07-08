package com.hubinity.catalog.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hubinity.catalog.domain.OutboxMessage;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.hubinity.catalog.domain.OutboxMessageRepository;
import com.hubinity.catalog.domain.Product;
import com.hubinity.catalog.domain.StockMovementType;
import com.hubinity.catalog.events.published.PriceChangedEvent;
import com.hubinity.catalog.events.published.ProductCreatedEvent;
import com.hubinity.catalog.events.published.ProductDeactivatedEvent;
import com.hubinity.catalog.events.published.ProductUpdatedEvent;
import com.hubinity.catalog.events.published.StockChangedEvent;

/**
 * Persiste eventos de domínio na tabela {@code outbox_messages} dentro da
 * <strong>mesma transação</strong> do chamador. O OutboxDispatcher lê o outbox
 * em lotes PENDING e publica no exchange {@code catalog.events}.
 *
 * <p>Se o negócio fizer rollback, o evento também é desfeito atomicamente.
 * A entrega é at-least-once — consumers DEVEM deduplicar por {@code messageId}
 * (= {@code eventId} do payload).
 *
 * <p>Os records em {@code events.published} espelham os schemas em
 * {@code platform-shared-contracts/contracts-events/events/v1/}.
 * TODO(contracts): migrar para os POJOs gerados por jsonschema2pojo quando o
 * contracts-events emitir anotações Jackson 3 ({@code tools.jackson}) e mapear
 * {@code date-time} para {@code Instant} — hoje o gerador produz Jackson 2
 * ({@code com.fasterxml}) e {@code Date}, o que exigiria conversores no catalog.
 */
@Service
public class DefaultEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventPublisher.class);

    /**
     * Mantido em 1.0.0: a reconciliação schema↔payload é correção in-place pré-GA
     * (contracts 0.1.0-SNAPSHOT sem consumidores — ADR 0005/0006); breaking real
     * exigiria novo path {@code events/v2/} (ADR 0007).
     */
    static final String SCHEMA_VERSION = "1.0.0";

    private final OutboxMessageRepository outboxMessages;
    private final JsonMapper jsonMapper;

    public DefaultEventPublisher(OutboxMessageRepository outboxMessages, JsonMapper jsonMapper) {
        this.outboxMessages = outboxMessages;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishProductCreated(Product product) {
        UUID eventId = UUID.randomUUID();
        var event = new ProductCreatedEvent(
                eventId, SCHEMA_VERSION,
                product.getId(), product.getSku(), product.getName(),
                product.getPrice(), product.getCategoryId(), product.isActive(), Instant.now());
        persist(CatalogEvent.PRODUCT_CREATED, product.getId().toString(), eventId, event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishProductUpdated(Product product) {
        UUID eventId = UUID.randomUUID();
        var event = new ProductUpdatedEvent(
                eventId, SCHEMA_VERSION,
                product.getId(), product.getSku(), product.getName(),
                product.getPrice(), product.getCategoryId(), product.isActive(), Instant.now());
        persist(CatalogEvent.PRODUCT_UPDATED, product.getId().toString(), eventId, event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishProductDeactivated(Product product) {
        UUID eventId = UUID.randomUUID();
        var event = new ProductDeactivatedEvent(
                eventId, SCHEMA_VERSION, product.getId(), product.getSku(), Instant.now());
        persist(CatalogEvent.PRODUCT_DEACTIVATED, product.getId().toString(), eventId, event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishStockChanged(UUID productId, int available, int reserved,
                                    StockMovementType changeType, int quantity, Instant occurredAt) {
        UUID eventId = UUID.randomUUID();
        var event = new StockChangedEvent(
                eventId,
                SCHEMA_VERSION,
                productId,
                previousAvailable(changeType, available, quantity),
                previousReserved(changeType, reserved, quantity),
                available,
                reserved,
                changeType,
                quantity,
                occurredAt);
        persist(CatalogEvent.STOCK_CHANGED, productId.toString(), eventId, event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishPriceChanged(Product product, BigDecimal previousPrice) {
        UUID eventId = UUID.randomUUID();
        var event = new PriceChangedEvent(
                eventId, SCHEMA_VERSION,
                product.getId(), product.getSku(), previousPrice, product.getPrice(), Instant.now());
        persist(CatalogEvent.PRICE_CHANGED, product.getId().toString(), eventId, event);
    }

    /**
     * @param eventId identificador do evento — usado como {@code messageId} do outbox
     *                E como campo {@code eventId} do payload, garantindo que a chave
     *                de deduplicação seja a mesma no header AMQP e no corpo.
     */
    private void persist(CatalogEvent catalogEvent, String aggregateId, UUID eventId, Object payload) {
        try {
            String json = jsonMapper.writeValueAsString(payload);
            OutboxMessage msg = new OutboxMessage(
                    eventId,
                    catalogEvent.aggregateType,
                    aggregateId,
                    catalogEvent.eventType,
                    catalogEvent.routingKey,
                    SCHEMA_VERSION,
                    json);
            outboxMessages.save(msg);
            log.debug("event=outbox_persisted routingKey={} aggregateId={} messageId={}",
                    catalogEvent.routingKey, aggregateId, msg.getMessageId());
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Falha ao serializar evento para outbox: " + catalogEvent.eventType, e);
        }
    }

    /**
     * Reconstrói o estado de {@code available} antes da mutação a partir do estado
     * pós-mutação. Evita leitura adicional ao banco — ver invariantes de cada tipo:
     * <ul>
     *   <li>IN:      available += qty (reserved inalterado)</li>
     *   <li>OUT:     available -= qty (reserved inalterado)</li>
     *   <li>RESERVE: available -= qty, reserved += qty</li>
     *   <li>RELEASE: available += qty, reserved -= qty</li>
     *   <li>COMMIT:  reserved  -= qty (available inalterado)</li>
     * </ul>
     */
    private static int previousAvailable(StockMovementType type, int available, int quantity) {
        return switch (type) {
            case IN      -> available - quantity;
            case OUT     -> available + quantity;
            case RESERVE -> available + quantity;
            case RELEASE -> available - quantity;
            case COMMIT  -> available;
        };
    }

    private static int previousReserved(StockMovementType type, int reserved, int quantity) {
        return switch (type) {
            case IN, OUT -> reserved;
            case RESERVE -> reserved - quantity;
            case RELEASE, COMMIT -> reserved + quantity;
        };
    }
}

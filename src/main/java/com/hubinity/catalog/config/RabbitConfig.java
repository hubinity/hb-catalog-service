package com.hubinity.catalog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestrutura RabbitMQ do catálogo.
 *
 * <p>Condicional em {@code spring.rabbitmq.host} para não exigir broker nos
 * context-load tests offline (application-test.yml exclui RabbitAutoConfiguration
 * e não define esse property). Testes de integração com broker real fornecem
 * o host via @DynamicPropertySource e ativam esta configuração.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    public static final String EXCHANGE = "catalog.events";
    public static final String DLX      = "catalog.events.dlx";
    public static final String DLQ      = "catalog.events.dlq";

    @Bean
    public TopicExchange catalogEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public FanoutExchange catalogEventsDlx() {
        return ExchangeBuilder.fanoutExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue catalogEventsDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding catalogEventsDlqBinding(Queue catalogEventsDlq, FanoutExchange catalogEventsDlx) {
        return BindingBuilder.bind(catalogEventsDlq).to(catalogEventsDlx);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        // mandatory=true activa o returnCallback para mensagens sem rota
        template.setMandatory(true);

        // Publisher confirms (CORRELATED): loga NACKs do broker.
        // O Outbox cuida do retry — confirms são para observabilidade.
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("event=rabbit_nack cause={} correlationData={}", cause, correlationData);
            }
        });

        // Return callback: mensagem não pôde ser roteada para nenhuma queue
        template.setReturnsCallback(returned ->
                log.error("event=rabbit_unroutable exchange={} routingKey={} replyCode={} replyText={}",
                        returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText()));

        return template;
    }
}

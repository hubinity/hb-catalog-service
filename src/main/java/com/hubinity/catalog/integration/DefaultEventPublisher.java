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
 * A entrega é at-least-once — consumers DEVEM deduplicar por {@code messageId}.
 */
@Service
public class DefaultEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventPublisher.class);

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
        var event = new ProductCreatedEvent(
                product.getId(), product.getSku(), product.getName(),
                product.getPrice(), product.getCategoryId(), product.isActive(), Instant.now());
        persist(CatalogEvent.PRODUCT_CREATED, product.getId().toString(), event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishProductUpdated(Product product) {
        var event = new ProductUpdatedEvent(
                product.getId(), product.getSku(), product.getName(),
                product.getPrice(), product.getCategoryId(), product.isActive(), Instant.now());
        persist(CatalogEvent.PRODUCT_UPDATED, product.getId().toString(), event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishProductDeactivated(Product product) {
        var event = new ProductDeactivatedEvent(product.getId(), product.getSku(), Instant.now());
        persist(CatalogEvent.PRODUCT_DEACTIVATED, product.getId().toString(), event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishStockChanged(UUID productId, int available, int reserved,
                                    StockMovementType changeType, int quantity, Instant occurredAt) {
        var event = new StockChangedEvent(
                productId,
                previousAvailable(changeType, available, quantity),
                previousReserved(changeType, reserved, quantity),
                available,
                reserved,
                changeType,
                quantity,
                occurredAt);
        persist(CatalogEvent.STOCK_CHANGED, productId.toString(), event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishPriceChanged(Product product, BigDecimal previousPrice) {
        var event = new PriceChangedEvent(
                product.getId(), product.getSku(), previousPrice, product.getPrice(), Instant.now());
        persist(CatalogEvent.PRICE_CHANGED, product.getId().toString(), event);
    }

    private void persist(CatalogEvent catalogEvent, String aggregateId, Object payload) {
        try {
            String json = jsonMapper.writeValueAsString(payload);
            OutboxMessage msg = new OutboxMessage(
                    UUID.randomUUID(),
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

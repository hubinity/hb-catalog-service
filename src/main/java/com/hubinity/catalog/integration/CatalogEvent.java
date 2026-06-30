package com.hubinity.catalog.integration;

/**
 * Centraliza routing keys e event types do exchange {@code catalog.events}.
 * Usar constantes aqui garante consistência entre produtor (DefaultEventPublisher)
 * e qualquer consumidor que precise filtrar por tipo.
 */
public enum CatalogEvent {

    PRODUCT_CREATED("catalog.product.created", "ProductCreated", "Product"),
    PRODUCT_UPDATED("catalog.product.updated", "ProductUpdated", "Product"),
    PRODUCT_DEACTIVATED("catalog.product.deactivated", "ProductDeactivated", "Product"),
    STOCK_CHANGED("catalog.stock.changed", "StockChanged", "StockItem"),
    PRICE_CHANGED("catalog.price.changed", "PriceChanged", "Product");

    public final String routingKey;
    public final String eventType;
    public final String aggregateType;

    CatalogEvent(String routingKey, String eventType, String aggregateType) {
        this.routingKey = routingKey;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
    }
}

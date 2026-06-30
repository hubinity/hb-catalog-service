package com.hubinity.catalog.api.dto;

/**
 * Composite response for a recorded movement — lets a caller observe the resulting
 * {@code available}/{@code reserved} counters in the same response, without a separate
 * stock-snapshot endpoint.
 */
public record StockMovementResult(StockMovementResponse movement, StockItemResponse stock) {
}

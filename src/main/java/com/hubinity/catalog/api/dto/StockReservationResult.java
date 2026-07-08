package com.hubinity.catalog.api.dto;

/**
 * Composite response for reserve/release/commit — lets a caller observe the resulting
 * {@code available}/{@code reserved} counters in the same response, without a separate
 * stock-snapshot endpoint.
 */
public record StockReservationResult(StockReservationResponse reservation, StockItemResponse stock) {
}

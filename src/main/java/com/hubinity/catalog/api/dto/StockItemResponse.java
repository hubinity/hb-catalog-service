package com.hubinity.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Public projection of a {@code StockItem} (current counters) returned by the API. */
public record StockItemResponse(
        UUID productId,
        Integer available,
        Integer reserved,
        Integer reorderPoint,
        Instant updatedAt
) {
}

package com.hubinity.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.hubinity.catalog.domain.StockMovementType;

/** Public projection of a {@code StockMovement} journal row returned by the API. */
public record StockMovementResponse(
        UUID id,
        UUID productId,
        StockMovementType type,
        Integer quantity,
        String reason,
        Instant occurredAt,
        Instant createdAt,
        String createdBy
) {
}

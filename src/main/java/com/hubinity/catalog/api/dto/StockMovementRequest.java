package com.hubinity.catalog.api.dto;

import com.hubinity.catalog.domain.StockMovementType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request payload for recording a stock movement.
 *
 * <p>{@code productId} comes from the URL path — not from the body.
 */
public record StockMovementRequest(
        @NotNull StockMovementType type,
        @Positive int quantity,
        @Size(max = 120) String reason
) {
}

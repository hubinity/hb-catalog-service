package com.hubinity.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request payload for creating a new {@code StockReservation}. */
public record StockReservationRequest(
        @NotNull UUID productId,
        @Positive int quantity,
        @Size(max = 120) String externalRef,
        @NotNull @Future Instant expiresAt
) {
}

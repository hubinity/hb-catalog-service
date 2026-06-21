package com.hubinity.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.hubinity.catalog.domain.StockReservationStatus;

/** Public projection of a {@code StockReservation} returned by the API. */
public record StockReservationResponse(
        UUID id,
        UUID productId,
        Integer quantity,
        String externalRef,
        StockReservationStatus status,
        Instant expiresAt,
        Instant createdAt
) {
}

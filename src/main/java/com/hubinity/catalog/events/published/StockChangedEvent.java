package com.hubinity.catalog.events.published;

import java.time.Instant;
import java.util.UUID;

import com.hubinity.catalog.domain.StockMovementType;

import jakarta.validation.constraints.NotNull;

public record StockChangedEvent(
        @NotNull UUID productId,
        int previousAvailable,
        int previousReserved,
        int available,
        int reserved,
        @NotNull StockMovementType changeType,
        int quantity,
        @NotNull Instant occurredAt
) {}

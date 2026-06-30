package com.hubinity.catalog.events.published;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PriceChangedEvent(
        @NotNull UUID productId,
        @NotBlank String sku,
        @NotNull BigDecimal previousPrice,
        @NotNull BigDecimal newPrice,
        @NotNull Instant occurredAt
) {}

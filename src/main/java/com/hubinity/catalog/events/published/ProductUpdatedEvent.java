package com.hubinity.catalog.events.published;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductUpdatedEvent(
        @NotNull UUID productId,
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull BigDecimal price,
        @NotNull UUID categoryId,
        boolean active,
        @NotNull Instant occurredAt
) {}

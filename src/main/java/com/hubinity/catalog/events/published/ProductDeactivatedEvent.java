package com.hubinity.catalog.events.published;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductDeactivatedEvent(
        @NotNull UUID productId,
        @NotBlank String sku,
        @NotNull Instant occurredAt
) {}

package com.hubinity.catalog.events.published;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de {@code catalog.product.created} — espelha
 * {@code contracts-events/events/v1/ProductCreated.schema.json}.
 *
 * @param eventId       = messageId do outbox; chave de deduplicação at-least-once
 * @param schemaVersion versão SemVer do contrato (ADR 0005/0007)
 */
public record ProductCreatedEvent(
        @NotNull UUID eventId,
        @NotBlank String schemaVersion,
        @NotNull UUID productId,
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull BigDecimal price,
        @NotNull UUID categoryId,
        boolean active,
        @NotNull Instant occurredAt
) {}

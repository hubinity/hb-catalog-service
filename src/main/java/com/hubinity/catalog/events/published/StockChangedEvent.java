package com.hubinity.catalog.events.published;

import java.time.Instant;
import java.util.UUID;

import com.hubinity.catalog.domain.StockMovementType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de {@code catalog.stock.changed} — espelha
 * {@code contracts-events/events/v1/StockChanged.schema.json}.
 *
 * @param eventId       = messageId do outbox; chave de deduplicação at-least-once
 * @param schemaVersion versão SemVer do contrato (ADR 0005/0007)
 */
public record StockChangedEvent(
        @NotNull UUID eventId,
        @NotBlank String schemaVersion,
        @NotNull UUID productId,
        int previousAvailable,
        int previousReserved,
        int available,
        int reserved,
        @NotNull StockMovementType changeType,
        int quantity,
        @NotNull Instant occurredAt
) {}

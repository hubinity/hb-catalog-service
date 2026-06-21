package com.hubinity.catalog.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One historical price a product held, newest-first when listed. */
public record PriceHistoryResponse(
        UUID id,
        UUID productId,
        BigDecimal price,
        Instant createdAt
) {
}

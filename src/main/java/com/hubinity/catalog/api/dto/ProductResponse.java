package com.hubinity.catalog.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Public projection of a {@code Product} returned by the API. */
public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        BigDecimal costPrice,
        UUID categoryId,
        Boolean active,
        String barcode,
        Instant createdAt,
        Instant updatedAt
) {
}

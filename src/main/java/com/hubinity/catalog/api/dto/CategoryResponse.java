package com.hubinity.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Public projection of a {@code Category} returned by the API. */
public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        UUID parentId,
        Integer displayOrder,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

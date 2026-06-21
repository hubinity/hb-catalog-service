package com.hubinity.catalog.api.dto;

import java.util.List;

/** A page of {@link ProductResponse} results, with explicit pagination metadata. */
public record ProductPageResponse(
        List<ProductResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

package com.hubinity.catalog.api.dto;

import java.util.List;

/**
 * Explicit page envelope for {@code GET .../stock/movements} — mirrors
 * {@code ProductPageResponse} (002-product-rest-endpoints) rather than returning a raw Spring
 * {@code Page<T>}, avoiding the same deprecated Jackson {@code PageImpl} serialization concern
 * that endpoint's research.md already identified.
 */
public record StockMovementPageResponse(
        List<StockMovementResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

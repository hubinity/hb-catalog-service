package com.hubinity.catalog.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request payload for creating or updating a {@code Product}. */
public record ProductRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
        @DecimalMin("0.00") BigDecimal costPrice,
        @NotNull UUID categoryId,
        Boolean active,
        @Size(max = 64) String barcode
) {
}

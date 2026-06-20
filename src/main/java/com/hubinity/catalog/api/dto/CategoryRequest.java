package com.hubinity.catalog.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for creating or updating a {@code Category}. */
public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 150) String slug,
        UUID parentId,
        Integer displayOrder,
        Boolean active
) {
}

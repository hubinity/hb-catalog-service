package com.hubinity.catalog.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request payload for creating or updating a {@code Category}. */
public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "slug must be lowercase letters, digits, and hyphens only, with no leading, trailing, or double hyphens")
        String slug,
        UUID parentId,
        Integer displayOrder,
        Boolean active
) {
}

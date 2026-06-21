package com.hubinity.catalog.api.dto;

import java.util.List;
import java.util.UUID;

/** A {@code Category} nested with its children, for the {@code ?tree=true} view. */
public record CategoryTreeNode(
        UUID id,
        String name,
        String slug,
        Integer displayOrder,
        Boolean active,
        List<CategoryTreeNode> children
) {
}

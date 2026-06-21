package com.hubinity.catalog.api.error;

import java.util.UUID;

/** Thrown when a create/update request's {@code categoryId} does not reference an existing, alive category. */
public class InvalidCategoryException extends RuntimeException {

    public InvalidCategoryException(UUID categoryId) {
        super("Category not found: " + categoryId);
    }
}

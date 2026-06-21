package com.hubinity.catalog.api.error;

import java.util.UUID;

/** Thrown when a lookup, update, or removal targets an unknown or removed category id. */
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID id) {
        super("Category not found: " + id);
    }
}

package com.hubinity.catalog.api.error;

import java.util.UUID;

/** Thrown when removal is attempted on a category that still has one or more linked (non-removed) products. */
public class CategoryHasProductsException extends RuntimeException {

    public CategoryHasProductsException(UUID id) {
        super("Category " + id + " has one or more linked products and cannot be removed.");
    }
}

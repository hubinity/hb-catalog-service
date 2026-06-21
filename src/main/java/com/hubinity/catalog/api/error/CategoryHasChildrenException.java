package com.hubinity.catalog.api.error;

import java.util.UUID;

/** Thrown when removal is attempted on a category that still has one or more non-removed children. */
public class CategoryHasChildrenException extends RuntimeException {

    public CategoryHasChildrenException(UUID id) {
        super("Category " + id + " has one or more child categories and cannot be removed.");
    }
}

package com.hubinity.catalog.api.error;

import java.util.UUID;

/** Thrown when an update would make a category an ancestor of itself (direct or indirect). */
public class CircularReferenceException extends RuntimeException {

    public CircularReferenceException(UUID categoryId, UUID proposedParentId) {
        super("Category " + categoryId + " cannot be reparented under " + proposedParentId
                + " — it would create a circular reference.");
    }
}

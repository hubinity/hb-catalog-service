package com.hubinity.catalog.api.error;

import java.util.UUID;

/** Thrown when a create/update request's {@code parentId} does not reference an existing, alive category. */
public class InvalidParentException extends RuntimeException {

    public InvalidParentException(UUID parentId) {
        super("Parent category not found: " + parentId);
    }
}

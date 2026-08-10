package com.hubinity.catalog.domain.error;

/** Thrown when a create/update request's slug collides with an alive category's slug. */
public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException(String slug) {
        super("A category with slug '" + slug + "' already exists.");
    }
}

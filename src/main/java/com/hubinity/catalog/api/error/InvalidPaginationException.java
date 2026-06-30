package com.hubinity.catalog.api.error;

/** Thrown when a list/search request's page size is outside 1-100, or its sort field is not whitelisted. */
public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}

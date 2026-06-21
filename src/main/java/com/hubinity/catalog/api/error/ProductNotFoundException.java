package com.hubinity.catalog.api.error;

import java.util.UUID;

/** Thrown when a lookup, update, or removal targets an unknown or removed product id. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id) {
        super("Product not found: " + id);
    }
}

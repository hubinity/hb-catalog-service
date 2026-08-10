package com.hubinity.catalog.domain.error;

/** Thrown when a create/update request's SKU collides with an alive product's SKU. */
public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("A product with SKU '" + sku + "' already exists.");
    }
}

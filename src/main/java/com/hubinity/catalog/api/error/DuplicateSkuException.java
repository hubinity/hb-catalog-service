package com.hubinity.catalog.api.error;

/** Thrown when a create/update request's SKU collides with an alive product's SKU. */
public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("A product with SKU '" + sku + "' already exists.");
    }
}

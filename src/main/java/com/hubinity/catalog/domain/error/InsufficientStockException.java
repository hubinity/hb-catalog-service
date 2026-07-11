package com.hubinity.catalog.domain.error;

import java.util.UUID;

/** Thrown when an OUT movement or a reservation requests more than a product's current available quantity. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId, int requested, int available) {
        super("Product " + productId + " has only " + available + " available, but " + requested + " were requested.");
    }
}

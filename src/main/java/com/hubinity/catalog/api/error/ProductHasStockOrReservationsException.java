package com.hubinity.catalog.api.error;

import java.util.UUID;

/** Thrown when removal is attempted on a product that still has stock on hand or active reservations. */
public class ProductHasStockOrReservationsException extends RuntimeException {

    public ProductHasStockOrReservationsException(UUID id) {
        super("Product " + id + " has stock on hand or active reservations and cannot be removed.");
    }
}

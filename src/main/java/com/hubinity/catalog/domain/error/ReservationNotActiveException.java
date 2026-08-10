package com.hubinity.catalog.domain.error;

import java.util.UUID;

import com.hubinity.catalog.domain.StockReservationStatus;

/** Thrown when a release or commit request targets a reservation that is already committed or released. */
public class ReservationNotActiveException extends RuntimeException {

    public ReservationNotActiveException(UUID id, StockReservationStatus currentStatus) {
        super("Reservation " + id + " is " + currentStatus + ", not ACTIVE, and cannot be released or committed.");
    }
}

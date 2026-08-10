package com.hubinity.catalog.domain.error;

import java.util.UUID;

/** Thrown when a release or commit request targets a reservation id that does not exist. */
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID id) {
        super("Reservation " + id + " was not found.");
    }
}

package com.hubinity.catalog.domain.error;

import java.util.UUID;

/**
 * Thrown when a release or commit request targets a reservation whose {@code expiresAt} has
 * already passed but had not yet been swept by the periodic cleanup job (FR-010) — the same
 * request that discovers this lazily also performs the expiry side effect before throwing.
 */
public class ReservationExpiredException extends RuntimeException {

    public ReservationExpiredException(UUID id) {
        super("Reservation " + id + " expired and can no longer be released or committed.");
    }
}

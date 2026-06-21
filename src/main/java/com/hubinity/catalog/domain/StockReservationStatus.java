package com.hubinity.catalog.domain;

/**
 * Lifecycle states of a {@link StockReservation}.
 *
 * <p>Mirrors the {@code CHECK (status IN (...))} constraint declared in
 * {@code V1__init.sql} on the {@code stock_reservation} table. Persisted as
 * {@code VARCHAR(16)} via {@code @Enumerated(EnumType.STRING)} so the column
 * stays diagnosable from psql.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — hold is in effect; counts against available stock.</li>
 *   <li>{@link #COMMITTED} — hold was converted into a sale.</li>
 *   <li>{@link #RELEASED} — hold was cancelled (caller-driven).</li>
 *   <li>{@link #EXPIRED} — TTL sweeper retired the hold past {@code expires_at}.</li>
 * </ul>
 */
public enum StockReservationStatus {
    ACTIVE,
    COMMITTED,
    RELEASED,
    EXPIRED
}

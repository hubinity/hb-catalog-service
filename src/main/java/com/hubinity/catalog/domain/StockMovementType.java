package com.hubinity.catalog.domain;

/**
 * Discriminator for {@link StockMovement} rows.
 *
 * <p>Mirrors the {@code CHECK (type IN (...))} constraint declared in
 * {@code V1__init.sql} on the {@code stock_movement} table. Persisted as
 * {@code VARCHAR(16)} via {@code @Enumerated(EnumType.STRING)} to keep the
 * journal human-readable in DB dumps and SQL audits.
 *
 * <ul>
 *   <li>{@link #IN} — stock entering the system (e.g. supplier receipt).</li>
 *   <li>{@link #OUT} — stock leaving without a reservation (manual adjust).</li>
 *   <li>{@link #RESERVE} — a {@link StockReservation} was opened.</li>
 *   <li>{@link #RELEASE} — an ACTIVE reservation was cancelled or expired.</li>
 *   <li>{@link #COMMIT} — an ACTIVE reservation was converted to a real sale.</li>
 * </ul>
 */
public enum StockMovementType {
    IN,
    OUT,
    RESERVE,
    RELEASE,
    COMMIT
}

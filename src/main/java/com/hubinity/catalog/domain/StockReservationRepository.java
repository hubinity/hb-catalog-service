package com.hubinity.catalog.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link StockReservation}.
 *
 * <p>The TTL sweeper finder ({@link #findByStatusAndExpiresAtBefore}) backs
 * the periodic job that flips overdue {@link StockReservationStatus#ACTIVE}
 * rows into {@link StockReservationStatus#EXPIRED}.
 */
@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByStatusAndExpiresAtBefore(StockReservationStatus status, Instant cutoff);

    Optional<StockReservation> findByExternalRef(String externalRef);

    boolean existsByProductIdAndStatus(UUID productId, StockReservationStatus status);

    /**
     * The single transition primitive behind release, commit, lazy expiry,
     * and the sweep job (feature 003-stock-movement-reservation, FR-020):
     * a reservation can only ever leave {@code ACTIVE} once, so the
     * {@code status = ACTIVE} guard in the {@code WHERE} clause is itself
     * the race-safety mechanism — whichever caller's {@code UPDATE} executes
     * first wins; the loser affects 0 rows. {@code now} additionally gates
     * out an already-expired-but-not-yet-swept row, so a caller-driven
     * commit/release naturally fails here too (lazy expiry detection).
     * See specs/003-stock-movement-reservation/research.md.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StockReservation r SET r.status = :newStatus WHERE r.id = :id "
         + "AND r.status = com.hubinity.catalog.domain.StockReservationStatus.ACTIVE "
         + "AND r.expiresAt > :now")
    int transitionIfActive(UUID id, StockReservationStatus newStatus, Instant now);

    /**
     * The companion expiry transition: opposite comparison from
     * {@link #transitionIfActive} ({@code expiresAt <= now}, not {@code > now}),
     * since this is exactly the case that method's guard excludes. Used by
     * both the lazy-expiry path (commit/release discovering a past-due
     * reservation) and the {@code @Scheduled} sweep job.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StockReservation r SET r.status = com.hubinity.catalog.domain.StockReservationStatus.EXPIRED "
         + "WHERE r.id = :id AND r.status = com.hubinity.catalog.domain.StockReservationStatus.ACTIVE "
         + "AND r.expiresAt <= :now")
    int expireIfDue(UUID id, Instant now);
}

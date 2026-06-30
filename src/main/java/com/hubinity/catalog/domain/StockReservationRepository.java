package com.hubinity.catalog.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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
}

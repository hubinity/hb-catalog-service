package com.hubinity.catalog.domain;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link StockMovement}.
 *
 * <p>Append-only — there is no soft-delete or update path. The single finder
 * here serves the per-product history view (most recent first).
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findByProductIdOrderByOccurredAtDesc(UUID productId, Pageable pageable);
}

package com.hubinity.catalog.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

/**
 * Spring Data repository for {@link StockItem}.
 *
 * <p>The PK type is {@link UUID} — the same {@code product_id} that identifies
 * the parent {@link Product}.
 */
@Repository
public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    /**
     * Pessimistic write lock on the product's stock row, used by the
     * product-removal guard (FR-016) so the check and the soft-delete are
     * race-safe against a concurrently placed reservation. See
     * specs/002-product-rest-endpoints/research.md.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockItem s where s.productId = :productId")
    Optional<StockItem> findByIdForUpdate(UUID productId);
}

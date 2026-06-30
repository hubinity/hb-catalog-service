package com.hubinity.catalog.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link StockItem}.
 *
 * <p>The PK type is {@link UUID} — the same {@code product_id} that identifies
 * the parent {@link Product}.
 *
 * <p>The five {@code @Modifying} conditional-{@code UPDATE} methods below
 * (feature 003-stock-movement-reservation) are each the sole concurrency
 * primitive for their operation: the row-level write lock Postgres takes for
 * the duration of an {@code UPDATE} makes the affected-rows count (0 or 1)
 * an atomic, race-safe success/failure signal, with no explicit lock
 * management needed. See specs/003-stock-movement-reservation/research.md.
 *
 * <p>The product-removal guard (FR-016) previously used a
 * {@code @Lock(PESSIMISTIC_WRITE)} {@code findByIdForUpdate} here, which violated
 * this project's convention of using a single conditional {@code UPDATE} instead of
 * {@code SELECT ... FOR UPDATE} for concurrency checks. It has been replaced by
 * {@code ProductRepository#softDeleteIfRemovable}, an atomic conditional UPDATE that
 * performs the guard check and the soft-delete in one statement.
 */
@Repository
public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    /** IN movement — always succeeds if the row exists. */
    @Modifying
    @Query("UPDATE StockItem s SET s.available = s.available + :qty WHERE s.productId = :productId")
    int increaseAvailable(UUID productId, int qty);

    /** OUT movement — 0 rows affected means insufficient {@code available}. */
    @Modifying
    @Query("UPDATE StockItem s SET s.available = s.available - :qty WHERE s.productId = :productId AND s.available >= :qty")
    int decreaseAvailableIfSufficient(UUID productId, int qty);

    /** Reserve (FR-005) — 0 rows affected means insufficient {@code available}. */
    @Modifying
    @Query("UPDATE StockItem s SET s.available = s.available - :qty, s.reserved = s.reserved + :qty "
         + "WHERE s.productId = :productId AND s.available >= :qty")
    int reserveIfAvailable(UUID productId, int qty);

    /** Release a reservation, or return its quantity on expiry — shared by both paths. */
    @Modifying
    @Query("UPDATE StockItem s SET s.available = s.available + :qty, s.reserved = s.reserved - :qty "
         + "WHERE s.productId = :productId AND s.reserved >= :qty")
    int releaseReservedToAvailable(UUID productId, int qty);

    /** Commit a reservation — permanently deducts the held quantity from {@code reserved} only. */
    @Modifying
    @Query("UPDATE StockItem s SET s.reserved = s.reserved - :qty WHERE s.productId = :productId AND s.reserved >= :qty")
    int commitReserved(UUID productId, int qty);
}

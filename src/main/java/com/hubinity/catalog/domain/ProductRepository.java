package com.hubinity.catalog.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link Product}.
 *
 * <p>Also implements {@link JpaSpecificationExecutor} for the dynamic browse
 * filters that the service layer will add in feature 1.5.
 *
 * <p>The {@code @SQLRestriction("deleted_at IS NULL")} on the entity means
 * every finder here transparently scopes to alive rows.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    Page<Product> findByCategoryIdAndNameContainingIgnoreCase(UUID categoryId, String q, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String q, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsByCategoryId(UUID categoryId);

    /**
     * Product-removal guard (FR-016) as a single atomic conditional
     * {@code UPDATE}: the soft-delete only commits if, at the moment the
     * statement runs, the product has no stock on hand
     * ({@code available + reserved == 0}) and no {@code ACTIVE} reservation.
     * The row-level write lock Postgres holds for the duration of the
     * {@code UPDATE} makes the affected-rows count (0 or 1) an atomic,
     * race-safe success/failure signal — there is no read-then-act window,
     * unlike the previous {@code SELECT ... FOR UPDATE} approach. See
     * specs/002-product-rest-endpoints/research.md and the project
     * convention against reusing pessimistic locks for new concurrency
     * checks (CLAUDE.md, "Stock concurrency").
     */
    @Modifying
    @Query("""
        UPDATE Product p
        SET p.deletedAt = CURRENT_TIMESTAMP
        WHERE p.id = :id
          AND p.deletedAt IS NULL
          AND NOT EXISTS (
            SELECT 1 FROM StockItem si
            WHERE si.productId = :id AND (si.available + si.reserved) > 0
          )
          AND NOT EXISTS (
            SELECT 1 FROM StockReservation sr
            WHERE sr.productId = :id AND sr.status = com.hubinity.catalog.domain.StockReservationStatus.ACTIVE
          )
        """)
    int softDeleteIfRemovable(@Param("id") UUID id);
}

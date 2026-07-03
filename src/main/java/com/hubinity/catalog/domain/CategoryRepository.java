package com.hubinity.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link Category}.
 *
 * <p>The {@code @SQLRestriction("deleted_at IS NULL")} on the entity means
 * every finder here transparently scopes to alive rows.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIdOrderByDisplayOrderAsc(UUID parentId);

    List<Category> findByParentIdIsNullOrderByDisplayOrderAsc();

    boolean existsBySlug(String slug);

    boolean existsByParentId(UUID parentId);

    /**
     * Liveness assertion with teeth: a conditional no-op {@code UPDATE} that
     * acquires the category's row-level write lock ({@code FOR NO KEY UPDATE})
     * for the remainder of the caller's transaction. Unlike a plain
     * {@code SELECT} (which never blocks and is never blocked), this conflicts
     * with the soft-delete statement in {@link #softDeleteIfRemovable}, so a
     * transaction that links rows to this category (e.g. a product INSERT) and
     * a concurrent category delete serialize instead of interleaving invisibly
     * under READ COMMITTED. Note the FK's own {@code FOR KEY SHARE} check does
     * NOT provide this: it is compatible with the soft-delete's row lock.
     *
     * @return 1 if the category is alive (and its row now locked), 0 if it is
     *         missing or already soft-deleted.
     */
    @Modifying
    @Query("""
        UPDATE Category c
        SET c.updatedAt = c.updatedAt
        WHERE c.id = :id
          AND c.deletedAt IS NULL
        """)
    int touchIfAlive(@Param("id") UUID id);

    /**
     * Category-removal guard as a single atomic conditional {@code UPDATE}:
     * soft-deletes the category only if, at the moment the statement runs, it
     * is still alive, has no alive subcategory and no alive product linked to
     * it — guard and action in the same statement (same pattern as
     * {@link ProductRepository#softDeleteIfRemovable}; project convention:
     * atomic conditional {@code UPDATE}, never {@code SELECT ... FOR UPDATE}
     * — see CLAUDE.md, "Stock concurrency"). The row-level write lock Postgres
     * holds for the duration of the {@code UPDATE} makes the affected-rows
     * count (0 or 1) an atomic, race-safe success/failure signal against
     * concurrent INSERTs that validate the category in their own transaction.
     *
     * <p>The {@code deletedAt IS NULL} predicates on the subqueries are spelled
     * out explicitly rather than relying on {@code @SQLRestriction} being
     * injected into JPQL subqueries — duplicating the filter is harmless and
     * keeps the guard correct regardless of Hibernate's restriction handling.
     *
     * <p>{@code clearAutomatically = true} because the service re-reads the
     * state after a 0-rows outcome to pick the correct ProblemDetail — without
     * clearing the persistence context that re-read could come stale from the
     * first-level cache.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Category c
        SET c.deletedAt = CURRENT_TIMESTAMP
        WHERE c.id = :id
          AND c.deletedAt IS NULL
          AND NOT EXISTS (
            SELECT 1 FROM Category ch
            WHERE ch.parentId = :id AND ch.deletedAt IS NULL
          )
          AND NOT EXISTS (
            SELECT 1 FROM Product p
            WHERE p.categoryId = :id AND p.deletedAt IS NULL
          )
        """)
    int softDeleteIfRemovable(@Param("id") UUID id);
}

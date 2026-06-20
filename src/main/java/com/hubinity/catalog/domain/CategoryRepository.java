package com.hubinity.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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
}

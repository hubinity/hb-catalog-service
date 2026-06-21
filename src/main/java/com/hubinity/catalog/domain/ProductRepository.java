package com.hubinity.catalog.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}

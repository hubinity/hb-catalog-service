package com.hubinity.catalog.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link StockItem}.
 *
 * <p>The PK type is {@link UUID} — the same {@code product_id} that identifies
 * the parent {@link Product}.
 */
@Repository
public interface StockItemRepository extends JpaRepository<StockItem, UUID> {
}

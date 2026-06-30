package com.hubinity.catalog.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Current stock counters for a {@link Product} — one row per product.
 *
 * <p>The PK is the {@code product_id} FK itself (no separate identity column),
 * so {@link Product} and {@link StockItem} live in a 1:1 share-the-PK relation.
 * Unlike the other entities, {@code uuidv7()} is NOT used as a DB default here
 * — the FK value must be supplied at INSERT time. Consequently this entity
 * does NOT carry the {@code @Generated(INSERT)} hint that the other entities
 * use; Hibernate writes the {@code product_id} verbatim.
 *
 * <p>No {@code deleted_at} — see {@code V1__init.sql} table comment.
 */
@Entity
@Table(name = "stock_item")
@EntityListeners(AuditingEntityListener.class)
public class StockItem {

    // PK matches the parent product. No uuidv7() default — caller supplies it.
    // See src/main/resources/db/migration/V1__init.sql (CREATE TABLE stock_item).
    @Id
    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "available", nullable = false)
    private int available = 0;

    @Column(name = "reserved", nullable = false)
    private int reserved = 0;

    @Column(name = "reorder_point", nullable = false)
    private int reorderPoint = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 120)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    public StockItem() {
    }

    public StockItem(UUID productId) {
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public int getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(int reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockItem other)) {
            return false;
        }
        return productId != null && productId.equals(other.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(productId);
    }
}

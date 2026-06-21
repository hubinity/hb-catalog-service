package com.hubinity.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One price a {@link Product} held during a specific period — an append-only
 * journal entry, mirroring {@link StockMovement}'s shape.
 *
 * <p>Immutability is structural: this class exposes no setters beyond the
 * constructor, and the service layer only ever calls {@code save(new
 * PriceHistory(...))}, never updates an existing row.
 */
@Entity
@Table(name = "price_history")
@EntityListeners(AuditingEntityListener.class)
public class PriceHistory {

    // DB default uuidv7() fires because @Generated(INSERT) tells Hibernate to
    // omit `id` from the INSERT statement and refresh it via RETURNING.
    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "price", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 120)
    private String createdBy;

    public PriceHistory() {
    }

    public PriceHistory(UUID productId, BigDecimal price) {
        this.productId = productId;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PriceHistory other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

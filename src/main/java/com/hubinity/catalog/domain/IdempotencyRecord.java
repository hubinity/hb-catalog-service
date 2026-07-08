package com.hubinity.catalog.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One client-supplied {@code Idempotency-Key} and the request/response it
 * was first claimed for (feature 003-stock-movement-reservation, FR-014–016,
 * FR-021).
 *
 * <p>Every write to this table goes through {@code IdempotencyRecordRepository}'s
 * native {@code claim}/{@code finalizeRecord}/{@code releaseClaim} queries —
 * never through {@code save()} — because the claim-row pattern needs
 * {@code INSERT ... ON CONFLICT DO NOTHING} semantics that Spring Data's
 * derived methods can't express. This entity exists only to back
 * {@code findById} reads (the replay/conflict/expired-reclaim checks in
 * {@code IdempotencyFilter}). {@code responseStatus = 0} is the reserved
 * "claimed, pending" sentinel — see
 * specs/003-stock-movement-reservation/research.md ("Idempotency storage").
 */
@Entity
@Table(name = "idempotency_key")
public class IdempotencyRecord {

    @Id
    @Column(name = "key", updatable = false, nullable = false, length = 120)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false)
    private String responseBody;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public IdempotencyRecord() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdempotencyRecord other)) {
            return false;
        }
        return key != null && key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }
}

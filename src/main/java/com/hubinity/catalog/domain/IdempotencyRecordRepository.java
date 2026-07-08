package com.hubinity.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link IdempotencyRecord}.
 *
 * <p>Native queries throughout — Spring Data's derived-method dialect has no
 * {@code INSERT ... ON CONFLICT} equivalent, which the claim-row pattern
 * requires. See {@code IdempotencyFilter} and
 * specs/003-stock-movement-reservation/research.md ("Idempotency storage").
 */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    /**
     * Attempts to claim {@code key} with a "pending" placeholder row
     * ({@code response_status = 0}). Returns {@code 1} if this call claimed
     * it, {@code 0} if a row for {@code key} already exists (caller must
     * then read it to decide: still pending, matching replay, conflicting
     * reuse, or stale enough to re-claim).
     */
    @Modifying
    @Query(value = "INSERT INTO idempotency_key (key, request_hash, response_status, response_body) "
                 + "VALUES (:key, :requestHash, 0, '') ON CONFLICT (key) DO NOTHING", nativeQuery = true)
    int claim(String key, String requestHash);

    /**
     * Overwrites an existing row (whether it's our own just-claimed
     * placeholder, or a stale row older than 24h being re-claimed) with the
     * final request hash and response. {@code status >= 500} responses are
     * never written here — the filter calls {@link #releaseClaim} instead.
     */
    @Modifying
    @Query(value = "UPDATE idempotency_key SET request_hash = :requestHash, response_status = :status, "
                 + "response_body = :body, created_at = NOW() WHERE key = :key", nativeQuery = true)
    int finalizeRecord(String key, String requestHash, int status, String body);

    /**
     * Removes a "pending" placeholder row, used when the claimed request
     * failed with a transient (5xx) error so a retry can claim the key
     * fresh rather than replaying a non-final response.
     */
    @Modifying
    @Query(value = "DELETE FROM idempotency_key WHERE key = :key AND response_status = 0", nativeQuery = true)
    void releaseClaim(String key);
}

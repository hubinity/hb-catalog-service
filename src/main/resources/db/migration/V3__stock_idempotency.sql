-- =====================================================================
-- V3__stock_idempotency.sql — idempotency-key journal + StockItem backfill
--
-- Feature: 003-stock-movement-reservation
--
-- Scope of this migration:
--   * idempotency_key table — backs the IdempotencyFilter's claim-row
--     pattern for the four mutating stock endpoints (FR-014–016, FR-021).
--     No deleted_at/purge — stale rows are treated as expired at lookup
--     time once older than 24h; see research.md "Idempotency storage".
--   * Backfill: provision a stock_item row for any product created before
--     this feature shipped (ProductService.create() now does this for new
--     products going forward — see research.md "Missing StockItem
--     provisioning").
-- =====================================================================

CREATE TABLE idempotency_key (
    key             varchar(120) PRIMARY KEY,
    request_hash    varchar(64)  NOT NULL,
    response_status integer      NOT NULL,
    response_body   text         NOT NULL,
    created_at      timestamptz  NOT NULL DEFAULT NOW()
);

-- Lookup support for any future age-based maintenance; not required by
-- the lazy "claim afresh if older than 24h" read path itself, but cheap
-- and useful for ops queries.
CREATE INDEX ix_idempotency_key_created_at ON idempotency_key (created_at);

-- Backfill: every product created before this feature shipped (via
-- 002's tests/dev usage) never got a stock_item row, since
-- ProductService.create() only provisioned Product + PriceHistory.
INSERT INTO stock_item (product_id)
SELECT id FROM product WHERE id NOT IN (SELECT product_id FROM stock_item);

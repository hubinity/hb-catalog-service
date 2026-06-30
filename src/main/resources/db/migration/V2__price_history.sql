-- =====================================================================
-- V2__price_history.sql — append-only price-history journal
--
-- Feature: 002-product-rest-endpoints
--
-- Scope of this migration:
--   * price_history table — one row per price a product has ever held.
--     Mirrors stock_movement's append-only journal shape: no updated_at,
--     updated_by, or deleted_at, because there is nothing to update or
--     soft-delete — immutability is structural (see ADR-style rationale
--     in specs/002-product-rest-endpoints/research.md).
-- =====================================================================

CREATE TABLE price_history (
    id          uuid           PRIMARY KEY DEFAULT uuidv7(),
    product_id  uuid           NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    price       numeric(12, 2) NOT NULL CHECK (price >= 0),
    created_at  timestamptz    NOT NULL DEFAULT NOW(),
    created_by  varchar(120)   NULL
);

-- Chronological retrieval: newest price first per product.
CREATE INDEX ix_price_history_product_created
    ON price_history (product_id, created_at DESC);

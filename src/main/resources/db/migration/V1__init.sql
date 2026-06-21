-- =====================================================================
-- V1__init.sql — hb-catalog-service initial schema
--
-- Feature: 6a330666f0a39bff9531a113 (Phase 1, task 1.3)
-- PRD ref: section 4.1 (HB Catalog — entities) and section 5 (NFRs).
-- ADR ref: docs/adr/0011-soft-delete-deleted-at.md — soft-delete policy.
--
-- Scope of this migration:
--   * pgcrypto extension (defensive — uuidv7() does not require it, but
--     downstream features may rely on gen_random_bytes()).
--   * uuidv7() plpgsql function — RFC 9562 v7 UUIDs used as PK defaults.
--   * set_updated_at() trigger function — auto-bumps audit timestamps.
--   * Five business tables: category, product, stock_item, stock_movement,
--     stock_reservation. UUID v7 PKs across the board.
--
-- Intentionally out of scope:
--   * Product image storage (PRD 4.1 keeps image references soft for MVP).
--   * Seed data — tests will provision rows explicitly.
--   * JPA entity mappings — see task 1.4.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Extensions
-- ---------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ---------------------------------------------------------------------
-- 2. uuidv7() — RFC 9562 v7 UUID generator
--
-- Postgres 16 does not ship a native uuidv7(). This implementation
-- assembles the 128 bits as defined by RFC 9562 §5.7:
--     48 bits — unix_ts_ms (millisecond Unix timestamp)
--      4 bits — version field, constant 0x7
--     12 bits — rand_a
--      2 bits — variant field, constant 0b10 (encoded as nibble 8/9/a/b)
--     62 bits — rand_b
-- Inspired by https://github.com/dverite/postgres-uuidv7 (BSD-style notes).
-- ---------------------------------------------------------------------

CREATE OR REPLACE FUNCTION uuidv7() RETURNS uuid
LANGUAGE plpgsql
AS $$
DECLARE
    unix_ts_ms bigint := (extract(epoch from clock_timestamp()) * 1000)::bigint;
    uuid_bytes bytea;
BEGIN
    uuid_bytes := decode(
        lpad(to_hex(unix_ts_ms), 12, '0') ||
        '7' || lpad(to_hex((random() * 4095)::int), 3, '0') ||
        (array['8','9','a','b'])[1 + floor(random() * 4)::int] ||
        lpad(to_hex((random() * 268435455)::int), 7, '0') ||
        lpad(to_hex((random() * 4294967295)::bigint), 8, '0')
        , 'hex');
    RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$;

COMMENT ON FUNCTION uuidv7() IS
    'RFC 9562 v7 UUID generator. Used as DEFAULT for every PK column in this schema.';


-- ---------------------------------------------------------------------
-- 3. set_updated_at() — trigger function bumping updated_at on UPDATE
-- ---------------------------------------------------------------------

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION set_updated_at() IS
    'BEFORE UPDATE trigger: bumps updated_at to NOW() on every row update.';


-- ---------------------------------------------------------------------
-- 4. category — product categories (logical tree via parent_id)
-- ---------------------------------------------------------------------

CREATE TABLE category (
    id            uuid         PRIMARY KEY DEFAULT uuidv7(),
    name          varchar(120) NOT NULL,
    slug          varchar(150) NOT NULL,
    parent_id     uuid         NULL REFERENCES category(id) ON DELETE RESTRICT,
    display_order integer      NOT NULL DEFAULT 0,
    active        boolean      NOT NULL DEFAULT TRUE,
    created_at    timestamptz  NOT NULL DEFAULT NOW(),
    updated_at    timestamptz  NOT NULL DEFAULT NOW(),
    created_by    varchar(120) NULL,
    updated_by    varchar(120) NULL,
    deleted_at    timestamptz  NULL
);

-- Partial unique: slug is unique among alive rows only. Soft-deleting a
-- category frees its slug for reuse without a hard delete + re-INSERT.
CREATE UNIQUE INDEX ux_category_slug_alive
    ON category (slug)
    WHERE deleted_at IS NULL;

-- Tree retrieval: list children of a parent in display order.
CREATE INDEX ix_category_parent_order
    ON category (parent_id, display_order);

CREATE TRIGGER trg_category_set_updated_at
    BEFORE UPDATE ON category
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- 5. product — sellable items
-- ---------------------------------------------------------------------

CREATE TABLE product (
    id          uuid           PRIMARY KEY DEFAULT uuidv7(),
    sku         varchar(64)    NOT NULL,
    name        varchar(200)   NOT NULL,
    description text           NULL,
    price       numeric(12, 2) NOT NULL CHECK (price >= 0),
    cost_price  numeric(12, 2) NULL     CHECK (cost_price IS NULL OR cost_price >= 0),
    category_id uuid           NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
    active      boolean        NOT NULL DEFAULT TRUE,
    barcode     varchar(64)    NULL,
    created_at  timestamptz    NOT NULL DEFAULT NOW(),
    updated_at  timestamptz    NOT NULL DEFAULT NOW(),
    created_by  varchar(120)   NULL,
    updated_by  varchar(120)   NULL,
    deleted_at  timestamptz    NULL
);

-- Partial unique: SKU is unique among alive products only (reuse after
-- soft-delete is allowed; see ADR 0011).
CREATE UNIQUE INDEX ux_product_sku_alive
    ON product (sku)
    WHERE deleted_at IS NULL;

-- Browse listing — paginate alive products by category.
CREATE INDEX ix_product_category
    ON product (category_id);

-- Barcode scanning — sparse index, skip NULLs.
CREATE INDEX ix_product_barcode
    ON product (barcode)
    WHERE barcode IS NOT NULL;

CREATE TRIGGER trg_product_set_updated_at
    BEFORE UPDATE ON product
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- 6. stock_item — one row per product, current available/reserved counts
--
-- Lives as long as the product. No deleted_at — when a product is
-- soft-deleted, its stock_item stays for reporting and only goes away
-- when (and if) the product is hard-deleted (out of MVP scope).
-- ---------------------------------------------------------------------

CREATE TABLE stock_item (
    product_id     uuid         PRIMARY KEY REFERENCES product(id) ON DELETE RESTRICT,
    available      integer      NOT NULL DEFAULT 0 CHECK (available >= 0),
    reserved       integer      NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    reorder_point  integer      NOT NULL DEFAULT 0 CHECK (reorder_point >= 0),
    created_at     timestamptz  NOT NULL DEFAULT NOW(),
    updated_at     timestamptz  NOT NULL DEFAULT NOW(),
    created_by     varchar(120) NULL,
    updated_by     varchar(120) NULL
);

CREATE TRIGGER trg_stock_item_set_updated_at
    BEFORE UPDATE ON stock_item
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- 7. stock_movement — append-only journal of stock changes
--
-- No updated_at / updated_by / deleted_at — this is a journal. Every
-- mutation is a new row.
-- ---------------------------------------------------------------------

CREATE TABLE stock_movement (
    id           uuid         PRIMARY KEY DEFAULT uuidv7(),
    product_id   uuid         NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    type         varchar(16)  NOT NULL CHECK (type IN ('IN', 'OUT', 'RESERVE', 'RELEASE', 'COMMIT')),
    quantity     integer      NOT NULL CHECK (quantity > 0),
    reason       varchar(120) NULL,
    occurred_at  timestamptz  NOT NULL DEFAULT NOW(),
    created_at   timestamptz  NOT NULL DEFAULT NOW(),
    created_by   varchar(120) NULL
);

-- History lookup: most recent movement first per product.
CREATE INDEX ix_stock_movement_product_occurred
    ON stock_movement (product_id, occurred_at DESC);


-- ---------------------------------------------------------------------
-- 8. stock_reservation — short-lived holds against available stock
--
-- Owned by the upstream caller (typically sc-order-service) via
-- external_ref. A TTL sweeper job will EXPIRE active rows past
-- expires_at; see (status, expires_at) index.
-- ---------------------------------------------------------------------

CREATE TABLE stock_reservation (
    id            uuid         PRIMARY KEY DEFAULT uuidv7(),
    product_id    uuid         NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    quantity      integer      NOT NULL CHECK (quantity > 0),
    external_ref  varchar(120) NULL,
    status        varchar(16)  NOT NULL CHECK (status IN ('ACTIVE', 'COMMITTED', 'RELEASED', 'EXPIRED')),
    expires_at    timestamptz  NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT NOW(),
    updated_at    timestamptz  NOT NULL DEFAULT NOW(),
    created_by    varchar(120) NULL,
    updated_by    varchar(120) NULL
);

-- TTL sweeper: scan ACTIVE rows whose expires_at has passed.
CREATE INDEX ix_stock_reservation_status_expires
    ON stock_reservation (status, expires_at);

-- Upstream lookup by their order id / cart id.
CREATE INDEX ix_stock_reservation_external_ref
    ON stock_reservation (external_ref);

CREATE TRIGGER trg_stock_reservation_set_updated_at
    BEFORE UPDATE ON stock_reservation
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

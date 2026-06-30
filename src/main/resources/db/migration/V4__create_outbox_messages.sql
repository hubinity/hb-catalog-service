-- =====================================================================
-- V4__create_outbox_messages.sql — tabela de outbox para publicação
-- confiável de eventos de domínio no RabbitMQ (Transactional Outbox Pattern)
--
-- Feature: 004-outbox-rabbitmq
--
-- Cada mutação de negócio (criar produto, alterar estoque, etc.) insere
-- uma linha aqui na MESMA TX ACID. O OutboxDispatcher lê lotes PENDING com
-- FOR UPDATE SKIP LOCKED e publica no exchange catalog.events, garantindo
-- at-least-once delivery independente do estado do broker no momento da TX.
-- =====================================================================

CREATE TABLE outbox_messages (
    id             BIGSERIAL       PRIMARY KEY,
    message_id     UUID            NOT NULL UNIQUE,
    aggregate_type VARCHAR(64)     NOT NULL,
    aggregate_id   VARCHAR(64)     NOT NULL,
    event_type     VARCHAR(128)    NOT NULL,
    routing_key    VARCHAR(128)    NOT NULL,
    schema_version VARCHAR(16)     NOT NULL,
    payload        JSONB           NOT NULL,
    headers        JSONB,
    status         VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    attempts       INT             NOT NULL DEFAULT 0,
    last_error     TEXT,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMPTZ
);

-- Índice parcial: usado exclusivamente pelo dispatcher (WHERE status='PENDING').
-- created_at + id garante ordem FIFO dentro de cada batch.
CREATE INDEX ix_outbox_messages_pending
    ON outbox_messages (created_at, id)
    WHERE status = 'PENDING';

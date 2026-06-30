package com.hubinity.catalog.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private UUID messageId;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "routing_key", nullable = false, length = 128)
    private String routingKey;

    @Column(name = "schema_version", nullable = false, length = 16)
    private String schemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private String headers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxMessageStatus status = OutboxMessageStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxMessage() {
    }

    public OutboxMessage(UUID messageId, String aggregateType, String aggregateId,
                         String eventType, String routingKey, String schemaVersion, String payload) {
        this.messageId = messageId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.schemaVersion = schemaVersion;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markPublished(Instant at) {
        this.status = OutboxMessageStatus.PUBLISHED;
        this.publishedAt = at;
    }

    public void recordFailure(String error) {
        this.attempts++;
        // trunca para não exceder o campo TEXT em casos extremos
        this.lastError = (error != null && error.length() > 2000) ? error.substring(0, 2000) : error;
    }

    public void markFailed() {
        this.status = OutboxMessageStatus.FAILED;
    }

    public Long getId()                        { return id; }
    public UUID getMessageId()                 { return messageId; }
    public String getAggregateType()           { return aggregateType; }
    public String getAggregateId()             { return aggregateId; }
    public String getEventType()               { return eventType; }
    public String getRoutingKey()              { return routingKey; }
    public String getSchemaVersion()           { return schemaVersion; }
    public String getPayload()                 { return payload; }
    public String getHeaders()                 { return headers; }
    public OutboxMessageStatus getStatus()     { return status; }
    public int getAttempts()                   { return attempts; }
    public String getLastError()               { return lastError; }
    public Instant getCreatedAt()              { return createdAt; }
    public Instant getPublishedAt()            { return publishedAt; }
}

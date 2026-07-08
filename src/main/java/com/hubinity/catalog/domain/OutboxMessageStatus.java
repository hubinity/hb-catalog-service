package com.hubinity.catalog.domain;

public enum OutboxMessageStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

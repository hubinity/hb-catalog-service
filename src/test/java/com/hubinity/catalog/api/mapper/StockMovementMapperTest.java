package com.hubinity.catalog.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.hubinity.catalog.api.dto.StockMovementRequest;
import com.hubinity.catalog.api.dto.StockMovementResponse;
import com.hubinity.catalog.domain.StockMovement;
import com.hubinity.catalog.domain.StockMovementType;

@DisplayName("StockMovementMapper — unit")
class StockMovementMapperTest {

    private final StockMovementMapper mapper = Mappers.getMapper(StockMovementMapper.class);

    @Test
    void toEntity_setsProductIdFromArgument() {
        UUID productId = UUID.randomUUID();
        StockMovementRequest req = new StockMovementRequest(StockMovementType.IN, 10, "restock");

        StockMovement entity = mapper.toEntity(req, productId);

        assertThat(entity.getProductId()).isEqualTo(productId);
        assertThat(entity.getQuantity()).isEqualTo(10);
        assertThat(entity.getReason()).isEqualTo("restock");
    }

    @Test
    void toEntity_setsOccurredAtToNowApprox() {
        Instant before = Instant.now();
        StockMovementRequest req = new StockMovementRequest(StockMovementType.OUT, 1, null);

        StockMovement entity = mapper.toEntity(req, UUID.randomUUID());

        Instant after = Instant.now();
        assertThat(entity.getOccurredAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    void toEntity_preservesEnumType() {
        StockMovementRequest req = new StockMovementRequest(StockMovementType.OUT, 3, null);

        StockMovement entity = mapper.toEntity(req, UUID.randomUUID());

        assertThat(entity.getType()).isEqualTo(StockMovementType.OUT);
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurred = Instant.parse("2026-01-01T10:00:00Z");
        Instant created = Instant.parse("2026-01-01T10:00:01Z");
        StockMovement entity = new StockMovement();
        entity.setId(id);
        entity.setProductId(productId);
        entity.setType(StockMovementType.COMMIT);
        entity.setQuantity(7);
        entity.setReason("order-123");
        entity.setOccurredAt(occurred);
        entity.setCreatedAt(created);
        entity.setCreatedBy("alice");

        StockMovementResponse resp = mapper.toResponse(entity);

        assertThat(resp.id()).isEqualTo(id);
        assertThat(resp.productId()).isEqualTo(productId);
        assertThat(resp.type()).isEqualTo(StockMovementType.COMMIT);
        assertThat(resp.quantity()).isEqualTo(7);
        assertThat(resp.reason()).isEqualTo("order-123");
        assertThat(resp.occurredAt()).isEqualTo(occurred);
        assertThat(resp.createdAt()).isEqualTo(created);
        assertThat(resp.createdBy()).isEqualTo("alice");
    }
}

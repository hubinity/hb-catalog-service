package com.hubinity.catalog.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.hubinity.catalog.api.dto.StockReservationRequest;
import com.hubinity.catalog.api.dto.StockReservationResponse;
import com.hubinity.catalog.domain.StockReservation;
import com.hubinity.catalog.domain.StockReservationStatus;

@DisplayName("StockReservationMapper — unit")
class StockReservationMapperTest {

    private final StockReservationMapper mapper = Mappers.getMapper(StockReservationMapper.class);

    @Test
    void toEntity_alwaysSetsStatusToActive() {
        Instant expires = Instant.now().plus(15, ChronoUnit.MINUTES);
        StockReservationRequest req = new StockReservationRequest(
                UUID.randomUUID(), 2, "cart-1", expires);

        StockReservation entity = mapper.toEntity(req);

        assertThat(entity.getStatus()).isEqualTo(StockReservationStatus.ACTIVE);
    }

    @Test
    void toEntity_preservesExpiresAt() {
        Instant expires = Instant.parse("2026-12-31T23:59:59Z");
        StockReservationRequest req = new StockReservationRequest(
                UUID.randomUUID(), 1, null, expires);

        StockReservation entity = mapper.toEntity(req);

        assertThat(entity.getExpiresAt()).isEqualTo(expires);
    }

    @Test
    void toEntity_preservesExternalRef() {
        StockReservationRequest req = new StockReservationRequest(
                UUID.randomUUID(), 1, "ext-42",
                Instant.now().plus(5, ChronoUnit.MINUTES));

        StockReservation entity = mapper.toEntity(req);

        assertThat(entity.getExternalRef()).isEqualTo("ext-42");
        assertThat(entity.getQuantity()).isEqualTo(1);
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant expires = Instant.parse("2026-06-19T15:00:00Z");
        Instant created = Instant.parse("2026-06-19T14:00:00Z");
        StockReservation entity = new StockReservation();
        entity.setId(id);
        entity.setProductId(productId);
        entity.setQuantity(4);
        entity.setExternalRef("ext-1");
        entity.setStatus(StockReservationStatus.ACTIVE);
        entity.setExpiresAt(expires);
        entity.setCreatedAt(created);

        StockReservationResponse resp = mapper.toResponse(entity);

        assertThat(resp.id()).isEqualTo(id);
        assertThat(resp.productId()).isEqualTo(productId);
        assertThat(resp.quantity()).isEqualTo(4);
        assertThat(resp.externalRef()).isEqualTo("ext-1");
        assertThat(resp.status()).isEqualTo(StockReservationStatus.ACTIVE);
        assertThat(resp.expiresAt()).isEqualTo(expires);
        assertThat(resp.createdAt()).isEqualTo(created);
    }
}

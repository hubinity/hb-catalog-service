package com.hubinity.catalog.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.hubinity.catalog.api.dto.StockItemResponse;
import com.hubinity.catalog.domain.StockItem;

@DisplayName("StockItemMapper — unit")
class StockItemMapperTest {

    private final StockItemMapper mapper = Mappers.getMapper(StockItemMapper.class);

    @Test
    void toResponse_mapsAllFields() {
        UUID productId = UUID.randomUUID();
        Instant updated = Instant.parse("2026-06-19T10:00:00Z");
        StockItem entity = new StockItem();
        entity.setProductId(productId);
        entity.setAvailable(20);
        entity.setReserved(3);
        entity.setReorderPoint(5);
        entity.setUpdatedAt(updated);

        StockItemResponse resp = mapper.toResponse(entity);

        assertThat(resp.productId()).isEqualTo(productId);
        assertThat(resp.available()).isEqualTo(20);
        assertThat(resp.reserved()).isEqualTo(3);
        assertThat(resp.reorderPoint()).isEqualTo(5);
        assertThat(resp.updatedAt()).isEqualTo(updated);
    }

    @Test
    void toResponse_handlesZeroCounters() {
        StockItem entity = new StockItem();
        entity.setProductId(UUID.randomUUID());
        // available/reserved/reorderPoint default to 0

        StockItemResponse resp = mapper.toResponse(entity);

        assertThat(resp.available()).isZero();
        assertThat(resp.reserved()).isZero();
        assertThat(resp.reorderPoint()).isZero();
    }

    @Test
    void toResponse_returnsNullForNullEntity() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}

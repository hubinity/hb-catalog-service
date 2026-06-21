package com.hubinity.catalog.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.dto.ProductResponse;
import com.hubinity.catalog.domain.Product;

@DisplayName("ProductMapper — unit")
class ProductMapperTest {

    private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

    @Test
    void toEntity_mapsAllProvidedFields() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest req = new ProductRequest(
                "SKU-001", "Latte", "house blend",
                new BigDecimal("19.99"), new BigDecimal("6.50"),
                categoryId, true, "789000000017");

        Product entity = mapper.toEntity(req);

        assertThat(entity.getSku()).isEqualTo("SKU-001");
        assertThat(entity.getName()).isEqualTo("Latte");
        assertThat(entity.getDescription()).isEqualTo("house blend");
        assertThat(entity.getPrice()).isEqualByComparingTo("19.99");
        assertThat(entity.getCostPrice()).isEqualByComparingTo("6.50");
        assertThat(entity.getCategoryId()).isEqualTo(categoryId);
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getBarcode()).isEqualTo("789000000017");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    void toEntity_defaultsActiveToTrueWhenNull() {
        ProductRequest req = new ProductRequest(
                "SKU-002", "Espresso", null,
                new BigDecimal("8.00"), null,
                UUID.randomUUID(), null, null);

        Product entity = mapper.toEntity(req);

        assertThat(entity.isActive()).isTrue();
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant created = Instant.parse("2026-01-01T10:00:00Z");
        Product entity = new Product();
        entity.setId(id);
        entity.setSku("SKU-003");
        entity.setName("Cappuccino");
        entity.setDescription("with foam");
        entity.setPrice(new BigDecimal("12.50"));
        entity.setCostPrice(new BigDecimal("4.25"));
        entity.setCategoryId(categoryId);
        entity.setActive(true);
        entity.setBarcode("789000000024");
        entity.setCreatedAt(created);
        entity.setUpdatedAt(created);

        ProductResponse resp = mapper.toResponse(entity);

        assertThat(resp.id()).isEqualTo(id);
        assertThat(resp.sku()).isEqualTo("SKU-003");
        assertThat(resp.name()).isEqualTo("Cappuccino");
        assertThat(resp.description()).isEqualTo("with foam");
        assertThat(resp.price()).isEqualByComparingTo("12.50");
        assertThat(resp.costPrice()).isEqualByComparingTo("4.25");
        assertThat(resp.categoryId()).isEqualTo(categoryId);
        assertThat(resp.active()).isTrue();
        assertThat(resp.barcode()).isEqualTo("789000000024");
        assertThat(resp.createdAt()).isEqualTo(created);
        assertThat(resp.updatedAt()).isEqualTo(created);
    }

    @Test
    void toEntity_ignoresIdAndAuditFields() {
        ProductRequest req = new ProductRequest(
                "SKU-004", "Mocha", null,
                new BigDecimal("15.00"), null,
                UUID.randomUUID(), true, null);

        Product entity = mapper.toEntity(req);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    void updateEntity_ignoresNullsFromRequest() {
        Product existing = new Product();
        existing.setName("foo");
        existing.setSku("SKU-OLD");
        existing.setPrice(new BigDecimal("10.00"));
        existing.setActive(true);

        ProductRequest patch = new ProductRequest(
                null, null, null,
                new BigDecimal("11.00"), null,
                UUID.randomUUID(), null, null);

        mapper.updateEntity(existing, patch);

        assertThat(existing.getName()).isEqualTo("foo");
        assertThat(existing.getSku()).isEqualTo("SKU-OLD");
        assertThat(existing.getPrice()).isEqualByComparingTo("11.00");
        assertThat(existing.isActive()).isTrue();
    }
}

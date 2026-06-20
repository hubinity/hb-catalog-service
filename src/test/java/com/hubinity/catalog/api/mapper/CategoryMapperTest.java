package com.hubinity.catalog.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.hubinity.catalog.api.dto.CategoryRequest;
import com.hubinity.catalog.api.dto.CategoryResponse;
import com.hubinity.catalog.domain.Category;

@DisplayName("CategoryMapper — unit")
class CategoryMapperTest {

    private final CategoryMapper mapper = Mappers.getMapper(CategoryMapper.class);

    @Test
    void toEntity_mapsAllProvidedFields() {
        UUID parentId = UUID.randomUUID();
        CategoryRequest req = new CategoryRequest("Drinks", "drinks", parentId, 5, false);

        Category entity = mapper.toEntity(req);

        assertThat(entity.getName()).isEqualTo("Drinks");
        assertThat(entity.getSlug()).isEqualTo("drinks");
        assertThat(entity.getParentId()).isEqualTo(parentId);
        assertThat(entity.getDisplayOrder()).isEqualTo(5);
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    void toEntity_defaultsDisplayOrderToZeroWhenNull() {
        CategoryRequest req = new CategoryRequest("Snacks", "snacks", null, null, true);

        Category entity = mapper.toEntity(req);

        assertThat(entity.getDisplayOrder()).isZero();
    }

    @Test
    void toEntity_defaultsActiveToTrueWhenNull() {
        CategoryRequest req = new CategoryRequest("Snacks", "snacks", null, 0, null);

        Category entity = mapper.toEntity(req);

        assertThat(entity.isActive()).isTrue();
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Instant created = Instant.parse("2026-01-01T10:00:00Z");
        Instant updated = Instant.parse("2026-01-02T10:00:00Z");
        Category entity = new Category();
        entity.setId(id);
        entity.setName("Drinks");
        entity.setSlug("drinks");
        entity.setParentId(parentId);
        entity.setDisplayOrder(3);
        entity.setActive(true);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);

        CategoryResponse resp = mapper.toResponse(entity);

        assertThat(resp.id()).isEqualTo(id);
        assertThat(resp.name()).isEqualTo("Drinks");
        assertThat(resp.slug()).isEqualTo("drinks");
        assertThat(resp.parentId()).isEqualTo(parentId);
        assertThat(resp.displayOrder()).isEqualTo(3);
        assertThat(resp.active()).isTrue();
        assertThat(resp.createdAt()).isEqualTo(created);
        assertThat(resp.updatedAt()).isEqualTo(updated);
    }

    @Test
    void updateEntity_ignoresNullsFromRequest() {
        Category existing = new Category();
        existing.setName("foo");
        existing.setSlug("foo-slug");
        existing.setDisplayOrder(7);
        existing.setActive(true);

        CategoryRequest patch = new CategoryRequest(null, "new-slug", null, null, null);

        mapper.updateEntity(existing, patch);

        assertThat(existing.getName()).isEqualTo("foo");
        assertThat(existing.getSlug()).isEqualTo("new-slug");
        assertThat(existing.getDisplayOrder()).isEqualTo(7);
        assertThat(existing.isActive()).isTrue();
    }
}

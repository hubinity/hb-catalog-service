package com.hubinity.catalog.api.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.hubinity.catalog.api.dto.StockMovementRequest;
import com.hubinity.catalog.api.dto.StockMovementResponse;
import com.hubinity.catalog.domain.StockMovement;

/**
 * MapStruct mapper for {@link StockMovement} entity ↔ DTOs.
 *
 * <p>{@code productId} arrives separately from the URL path. {@code occurredAt}
 * is stamped at mapping time. See ADR 0010
 * ({@code docs/adr/0010-mapstruct-vs-manual-mappers.md}).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockMovementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "type", source = "req.type")
    @Mapping(target = "quantity", source = "req.quantity")
    @Mapping(target = "reason", source = "req.reason")
    @Mapping(target = "occurredAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    StockMovement toEntity(StockMovementRequest req, UUID productId);

    StockMovementResponse toResponse(StockMovement entity);
}

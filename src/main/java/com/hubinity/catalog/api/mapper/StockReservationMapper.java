package com.hubinity.catalog.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.hubinity.catalog.api.dto.StockReservationRequest;
import com.hubinity.catalog.api.dto.StockReservationResponse;
import com.hubinity.catalog.domain.StockReservation;

/**
 * MapStruct mapper for {@link StockReservation} entity ↔ DTOs.
 *
 * <p>New reservations always start in {@code ACTIVE}. See ADR 0010
 * ({@code docs/adr/0010-mapstruct-vs-manual-mappers.md}).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockReservationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(com.hubinity.catalog.domain.StockReservationStatus.ACTIVE)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    StockReservation toEntity(StockReservationRequest req);

    StockReservationResponse toResponse(StockReservation entity);
}

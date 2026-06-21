package com.hubinity.catalog.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.hubinity.catalog.api.dto.StockItemResponse;
import com.hubinity.catalog.domain.StockItem;

/**
 * MapStruct mapper for {@link StockItem} → {@link StockItemResponse} (read-only).
 *
 * <p>See ADR 0010 ({@code docs/adr/0010-mapstruct-vs-manual-mappers.md}).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockItemMapper {

    StockItemResponse toResponse(StockItem entity);
}

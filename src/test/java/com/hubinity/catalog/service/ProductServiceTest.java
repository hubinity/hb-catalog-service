package com.hubinity.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.dto.ProductResponse;
import com.hubinity.catalog.api.error.DuplicateSkuException;
import com.hubinity.catalog.api.error.InvalidCategoryException;
import com.hubinity.catalog.api.error.ProductNotFoundException;
import com.hubinity.catalog.api.mapper.ProductMapper;
import com.hubinity.catalog.domain.CategoryRepository;
import com.hubinity.catalog.domain.PriceHistory;
import com.hubinity.catalog.domain.PriceHistoryRepository;
import com.hubinity.catalog.domain.Product;
import com.hubinity.catalog.domain.ProductRepository;
import com.hubinity.catalog.domain.StockItemRepository;
import com.hubinity.catalog.domain.StockReservationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private ProductMapper mapper;

    @Mock
    private CategoryRepository categories;

    @Mock
    private PriceHistoryRepository priceHistory;

    @Mock
    private StockItemRepository stockItems;

    @Mock
    private StockReservationRepository stockReservations;

    private ProductService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new ProductService(products, mapper, categories, priceHistory, stockItems, stockReservations);
    }

    private Product entityWithPrice(BigDecimal price) {
        Product p = new Product();
        p.setPrice(price);
        return p;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("valid create persists the product and writes one initial PriceHistory entry")
        void validCreate_persistsAndWritesInitialPriceHistory() {
            UUID categoryId = UUID.randomUUID();
            ProductRequest req = new ProductRequest(
                    "SKU-001", "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null);
            when(categories.existsById(categoryId)).thenReturn(true);
            when(products.existsBySku("SKU-001")).thenReturn(false);
            Product mapped = entityWithPrice(new BigDecimal("9.90"));
            when(mapper.toEntity(req)).thenReturn(mapped);
            Product saved = entityWithPrice(new BigDecimal("9.90"));
            saved.setId(UUID.randomUUID());
            when(products.save(mapped)).thenReturn(saved);
            when(mapper.toResponse(saved)).thenReturn(
                    new ProductResponse(saved.getId(), "SKU-001", "Widget", null, new BigDecimal("9.90"), null,
                            categoryId, true, null, null, null));

            service.create(req);

            ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
            verify(priceHistory).save(captor.capture());
            assertThat(captor.getValue().getProductId()).isEqualTo(saved.getId());
            assertThat(captor.getValue().getPrice()).isEqualTo(new BigDecimal("9.90"));
        }

        @Test
        @DisplayName("duplicate sku throws DuplicateSkuException and never saves")
        void duplicateSku_throws() {
            UUID categoryId = UUID.randomUUID();
            ProductRequest req = new ProductRequest(
                    "SKU-001", "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null);
            when(categories.existsById(categoryId)).thenReturn(true);
            when(products.existsBySku("SKU-001")).thenReturn(true);

            assertThatThrownBy(() -> service.create(req)).isInstanceOf(DuplicateSkuException.class);
            verify(products, never()).save(any());
        }

        @Test
        @DisplayName("unknown categoryId throws InvalidCategoryException and never saves")
        void unknownCategory_throws() {
            UUID categoryId = UUID.randomUUID();
            ProductRequest req = new ProductRequest(
                    "SKU-001", "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null);
            when(categories.existsById(categoryId)).thenReturn(false);

            assertThatThrownBy(() -> service.create(req)).isInstanceOf(InvalidCategoryException.class);
            verify(products, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        private com.hubinity.catalog.domain.StockItem stockItem(int available, int reserved) {
            com.hubinity.catalog.domain.StockItem item = new com.hubinity.catalog.domain.StockItem();
            item.setAvailable(available);
            item.setReserved(reserved);
            return item;
        }

        @Test
        @DisplayName("no stock and no active reservation soft-deletes the product")
        void noStockNoReservation_softDeletes() {
            UUID id = UUID.randomUUID();
            Product existing = entityWithPrice(new BigDecimal("9.90"));
            existing.setId(id);
            when(products.findById(id)).thenReturn(Optional.of(existing));
            when(stockItems.findByIdForUpdate(id)).thenReturn(Optional.empty());
            when(stockReservations.existsByProductIdAndStatus(id, com.hubinity.catalog.domain.StockReservationStatus.ACTIVE))
                    .thenReturn(false);

            service.delete(id);

            assertThat(existing.getDeletedAt()).isNotNull();
            verify(products).save(existing);
        }

        @Test
        @DisplayName("available stock greater than zero blocks removal")
        void availableStock_blocks() {
            UUID id = UUID.randomUUID();
            Product existing = entityWithPrice(new BigDecimal("9.90"));
            existing.setId(id);
            when(products.findById(id)).thenReturn(Optional.of(existing));
            when(stockItems.findByIdForUpdate(id)).thenReturn(Optional.of(stockItem(5, 0)));

            assertThatThrownBy(() -> service.delete(id))
                    .isInstanceOf(com.hubinity.catalog.api.error.ProductHasStockOrReservationsException.class);
            verify(products, never()).save(any());
        }

        @Test
        @DisplayName("reserved stock greater than zero blocks removal")
        void reservedStock_blocks() {
            UUID id = UUID.randomUUID();
            Product existing = entityWithPrice(new BigDecimal("9.90"));
            existing.setId(id);
            when(products.findById(id)).thenReturn(Optional.of(existing));
            when(stockItems.findByIdForUpdate(id)).thenReturn(Optional.of(stockItem(0, 3)));

            assertThatThrownBy(() -> service.delete(id))
                    .isInstanceOf(com.hubinity.catalog.api.error.ProductHasStockOrReservationsException.class);
            verify(products, never()).save(any());
        }

        @Test
        @DisplayName("an ACTIVE reservation blocks removal even at zero stock")
        void activeReservation_blocksEvenWithZeroStock() {
            UUID id = UUID.randomUUID();
            Product existing = entityWithPrice(new BigDecimal("9.90"));
            existing.setId(id);
            when(products.findById(id)).thenReturn(Optional.of(existing));
            when(stockItems.findByIdForUpdate(id)).thenReturn(Optional.of(stockItem(0, 0)));
            when(stockReservations.existsByProductIdAndStatus(id, com.hubinity.catalog.domain.StockReservationStatus.ACTIVE))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.delete(id))
                    .isInstanceOf(com.hubinity.catalog.api.error.ProductHasStockOrReservationsException.class);
            verify(products, never()).save(any());
        }

        @Test
        @DisplayName("unknown id throws ProductNotFoundException")
        void unknownId_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(products.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Product existingProduct(UUID id, String sku, BigDecimal price) {
            Product p = entityWithPrice(price);
            p.setId(id);
            p.setSku(sku);
            return p;
        }

        @Test
        @DisplayName("non-price field update persists and writes no new PriceHistory entry")
        void nonPriceUpdate_persistsNoNewHistory() {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Product existing = existingProduct(id, "SKU-001", new BigDecimal("9.90"));
            ProductRequest req = new ProductRequest(
                    "SKU-001", "Widget Renamed", null, new BigDecimal("9.90"), null, categoryId, null, null);
            when(products.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsById(categoryId)).thenReturn(true);
            when(products.findBySku("SKU-001")).thenReturn(Optional.of(existing));
            when(products.save(existing)).thenReturn(existing);
            when(mapper.toResponse(existing)).thenReturn(
                    new ProductResponse(id, "SKU-001", "Widget Renamed", null, new BigDecimal("9.90"), null,
                            categoryId, true, null, null, null));

            service.update(id, req);

            verify(mapper).updateEntity(existing, req);
            verify(priceHistory, never()).save(any());
        }

        @Test
        @DisplayName("price change persists and writes exactly one new PriceHistory entry")
        void priceChange_writesOneNewHistoryEntry() {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Product existing = existingProduct(id, "SKU-001", new BigDecimal("9.90"));
            ProductRequest req = new ProductRequest(
                    "SKU-001", "Widget", null, new BigDecimal("14.90"), null, categoryId, null, null);
            when(products.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsById(categoryId)).thenReturn(true);
            when(products.findBySku("SKU-001")).thenReturn(Optional.of(existing));
            when(products.save(existing)).thenReturn(existing);
            when(mapper.toResponse(existing)).thenReturn(
                    new ProductResponse(id, "SKU-001", "Widget", null, new BigDecimal("14.90"), null,
                            categoryId, true, null, null, null));

            // simulate the mapper actually applying the new price onto the entity
            org.mockito.Mockito.doAnswer(invocation -> {
                existing.setPrice(new BigDecimal("14.90"));
                return null;
            }).when(mapper).updateEntity(existing, req);

            service.update(id, req);

            ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
            verify(priceHistory).save(captor.capture());
            assertThat(captor.getValue().getPrice()).isEqualByComparingTo("14.90");
        }

        @Test
        @DisplayName("SKU collision with another product (excluding self) throws DuplicateSkuException")
        void skuCollisionWithAnother_throws() {
            UUID id = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Product existing = existingProduct(id, "SKU-001", new BigDecimal("9.90"));
            Product other = existingProduct(otherId, "SKU-002", new BigDecimal("5.00"));
            ProductRequest req = new ProductRequest(
                    "SKU-002", "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null);
            when(products.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsById(categoryId)).thenReturn(true);
            when(products.findBySku("SKU-002")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(DuplicateSkuException.class);
            verify(products, never()).save(any());
        }

        @Test
        @DisplayName("unknown categoryId on update throws InvalidCategoryException")
        void unknownCategoryOnUpdate_throws() {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            Product existing = existingProduct(id, "SKU-001", new BigDecimal("9.90"));
            ProductRequest req = new ProductRequest(
                    "SKU-001", "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null);
            when(products.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsById(categoryId)).thenReturn(false);

            assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(InvalidCategoryException.class);
            verify(products, never()).save(any());
        }

        @Test
        @DisplayName("unknown id throws ProductNotFoundException")
        void unknownId_throwsNotFound() {
            UUID id = UUID.randomUUID();
            ProductRequest req = new ProductRequest(
                    "SKU-001", "Widget", null, new BigDecimal("9.90"), null, UUID.randomUUID(), null, null);
            when(products.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPriceHistory")
    class GetPriceHistory {

        @Test
        @DisplayName("returns all entries newest-first for an existing product")
        void returnsEntriesNewestFirst() {
            UUID productId = UUID.randomUUID();
            PriceHistory newer = new PriceHistory(productId, new BigDecimal("14.90"));
            PriceHistory older = new PriceHistory(productId, new BigDecimal("9.90"));
            when(priceHistory.findByProductIdOrderByCreatedAtDesc(productId))
                    .thenReturn(java.util.List.of(newer, older));

            var result = service.getPriceHistory(productId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).price()).isEqualByComparingTo("14.90");
            assertThat(result.get(1).price()).isEqualByComparingTo("9.90");
        }

        @Test
        @DisplayName("unknown id (zero rows) throws ProductNotFoundException")
        void unknownId_throwsNotFound() {
            UUID productId = UUID.randomUUID();
            when(priceHistory.findByProductIdOrderByCreatedAtDesc(productId)).thenReturn(java.util.List.of());

            assertThatThrownBy(() -> service.getPriceHistory(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        private Product product(String sku, String name, BigDecimal price) {
            Product p = new Product();
            p.setId(UUID.randomUUID());
            p.setSku(sku);
            p.setName(name);
            p.setPrice(price);
            return p;
        }

        @Test
        @DisplayName("returns a page with correct totals and maps content via the mapper, defaulting to name ascending")
        void returnsPageWithTotals() {
            Product a = product("SKU-A", "Apple", new BigDecimal("1.00"));
            org.springframework.data.domain.Page<Product> page =
                    new org.springframework.data.domain.PageImpl<>(java.util.List.of(a),
                            org.springframework.data.domain.PageRequest.of(0, 20), 1);
            when(products.findAll(
                    org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Product>>any(),
                    org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(page);
            when(mapper.toResponse(a)).thenReturn(
                    new ProductResponse(a.getId(), "SKU-A", "Apple", null, new BigDecimal("1.00"), null,
                            UUID.randomUUID(), true, null, null, null));

            var result = service.search(null, null, 0, 20, "name,asc");

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("page size of 101 throws InvalidPaginationException")
        void pageSizeTooLarge_throws() {
            assertThatThrownBy(() -> service.search(null, null, 0, 101, "name,asc"))
                    .isInstanceOf(com.hubinity.catalog.api.error.InvalidPaginationException.class);
        }

        @Test
        @DisplayName("page size of 0 throws InvalidPaginationException")
        void pageSizeZero_throws() {
            assertThatThrownBy(() -> service.search(null, null, 0, 0, "name,asc"))
                    .isInstanceOf(com.hubinity.catalog.api.error.InvalidPaginationException.class);
        }

        @Test
        @DisplayName("negative page size throws InvalidPaginationException")
        void pageSizeNegative_throws() {
            assertThatThrownBy(() -> service.search(null, null, 0, -1, "name,asc"))
                    .isInstanceOf(com.hubinity.catalog.api.error.InvalidPaginationException.class);
        }

        @Test
        @DisplayName("an unsupported sort field throws InvalidPaginationException")
        void unsupportedSortField_throws() {
            assertThatThrownBy(() -> service.search(null, null, 0, 20, "description,asc"))
                    .isInstanceOf(com.hubinity.catalog.api.error.InvalidPaginationException.class);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns the mapped response when the product exists")
        void found_returnsResponse() {
            UUID id = UUID.randomUUID();
            Product entity = entityWithPrice(new BigDecimal("9.90"));
            entity.setId(id);
            when(products.findById(id)).thenReturn(Optional.of(entity));
            ProductResponse response = new ProductResponse(
                    id, "SKU-001", "Widget", null, new BigDecimal("9.90"), null, UUID.randomUUID(), true, null, null, null);
            when(mapper.toResponse(entity)).thenReturn(response);

            assertThat(service.getById(id)).isEqualTo(response);
        }

        @Test
        @DisplayName("unknown id throws ProductNotFoundException")
        void unknown_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(products.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id)).isInstanceOf(ProductNotFoundException.class);
        }
    }
}

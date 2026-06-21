package com.hubinity.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.hubinity.catalog.api.dto.CategoryRequest;
import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.dto.ProductResponse;
import com.hubinity.catalog.service.CategoryService;
import com.hubinity.catalog.service.ProductService;

/**
 * Verifies {@code Product} CRUD persistence against a real Postgres 16 container,
 * including the SKU partial unique index's behavior under concurrent writes.
 * Mirrors {@code CategoryPersistenceIT} for container + property wiring.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri="
        }
)
@ActiveProfiles("integration")
@Testcontainers
@Tag("integration")
@DisplayName("Product persistence — integration")
class ProductPersistenceIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("hb_catalog_test")
                    .withUsername("hb_catalog")
                    .withPassword("hb_catalog");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository products;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StockItemRepository stockItems;

    @Autowired
    private StockReservationRepository stockReservations;

    private UUID newCategory(String prefix) {
        return categoryService.create(new CategoryRequest(prefix, prefix, null, null, null)).id();
    }

    @Test
    @DisplayName("create then fetch by id returns every submitted field plus one initial price-history entry")
    void create_and_fetch_roundtrip_persists_all_fields() {
        UUID categoryId = newCategory("roundtrip-cat-" + UUID.randomUUID());
        String sku = "ROUNDTRIP-" + UUID.randomUUID();
        ProductRequest req = new ProductRequest(sku, "Roundtrip Widget", "desc", new BigDecimal("19.90"), null,
                categoryId, false, "BARCODE-1");

        ProductResponse created = productService.create(req);
        ProductResponse fetched = productService.getById(created.id());

        assertThat(fetched.sku()).isEqualTo(sku);
        assertThat(fetched.name()).isEqualTo("Roundtrip Widget");
        assertThat(fetched.price()).isEqualByComparingTo("19.90");
        assertThat(fetched.categoryId()).isEqualTo(categoryId);
        assertThat(fetched.active()).isFalse();
        assertThat(fetched.barcode()).isEqualTo("BARCODE-1");
    }

    @Test
    @DisplayName("two concurrent inserts with the same SKU: exactly one succeeds at the DB constraint level")
    void concurrent_inserts_same_sku_only_one_succeeds() throws Exception {
        UUID categoryId = newCategory("concurrent-cat-" + UUID.randomUUID());
        String sku = "CONCURRENT-" + UUID.randomUUID();
        int attempts = 5;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                ready.countDown();
                go.await();
                try {
                    Product p = new Product();
                    p.setSku(sku);
                    p.setName("Race");
                    p.setPrice(new BigDecimal("1.00"));
                    p.setCategoryId(categoryId);
                    products.saveAndFlush(p);
                    return true;
                } catch (DataIntegrityViolationException e) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(pool.submit(task));
        }
        ready.await();
        go.countDown();

        long successes = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successes++;
            }
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(products.findBySku(sku)).isPresent();
    }

    @Test
    @DisplayName("category filter, name search, and SKU search each return only the correct matches")
    void search_filter_and_sku_search_return_correct_matches() {
        UUID catA = newCategory("search-cat-a-" + UUID.randomUUID());
        UUID catB = newCategory("search-cat-b-" + UUID.randomUUID());
        String suffix = UUID.randomUUID().toString();
        productService.create(new ProductRequest("APL-" + suffix, "Apple " + suffix, null,
                new BigDecimal("1.00"), null, catA, null, null));
        productService.create(new ProductRequest("BAN-" + suffix, "Banana " + suffix, null,
                new BigDecimal("2.00"), null, catB, null, null));

        var byCategory = productService.search(catA, null, 0, 20, "name,asc");
        assertThat(byCategory.content()).hasSize(1);
        assertThat(byCategory.content().get(0).sku()).startsWith("APL-");

        var byNameSearch = productService.search(null, "Banana " + suffix, 0, 20, "name,asc");
        assertThat(byNameSearch.content()).hasSize(1);
        assertThat(byNameSearch.content().get(0).sku()).startsWith("BAN-");

        var bySkuSearch = productService.search(null, "APL-" + suffix, 0, 20, "name,asc");
        assertThat(bySkuSearch.content()).hasSize(1);
        assertThat(bySkuSearch.content().get(0).sku()).startsWith("APL-");
    }

    @Test
    @DisplayName("three sequential price updates produce three ordered, newest-first price-history entries; a no-op update adds none")
    void sequential_price_updates_produce_ordered_history() {
        UUID categoryId = newCategory("history-cat-" + UUID.randomUUID());
        String sku = "HISTORY-" + UUID.randomUUID();
        ProductResponse created = productService.create(
                new ProductRequest(sku, "Widget", null, new BigDecimal("10.00"), null, categoryId, null, null));

        productService.update(created.id(),
                new ProductRequest(sku, "Widget", null, new BigDecimal("20.00"), null, categoryId, null, null));
        productService.update(created.id(),
                new ProductRequest(sku, "Widget", null, new BigDecimal("20.00"), null, categoryId, null, null));
        productService.update(created.id(),
                new ProductRequest(sku, "Widget", null, new BigDecimal("30.00"), null, categoryId, null, null));

        var history = productService.getPriceHistory(created.id());

        assertThat(history).hasSize(3);
        assertThat(history.get(0).price()).isEqualByComparingTo("30.00");
        assertThat(history.get(1).price()).isEqualByComparingTo("20.00");
        assertThat(history.get(2).price()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("delete is blocked while stock is on hand, then succeeds once stock is cleared")
    void delete_blocked_with_stock_then_succeeds_after_cleared() {
        UUID categoryId = newCategory("delete-stock-cat-" + UUID.randomUUID());
        ProductResponse product = productService.create(new ProductRequest(
                "DEL-STOCK-" + UUID.randomUUID(), "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null));

        StockItem item = new StockItem();
        item.setProductId(product.id());
        item.setAvailable(5);
        stockItems.saveAndFlush(item);

        assertThatThrownBy(() -> productService.delete(product.id()))
                .isInstanceOf(com.hubinity.catalog.api.error.ProductHasStockOrReservationsException.class);

        item.setAvailable(0);
        stockItems.saveAndFlush(item);

        productService.delete(product.id());

        // C1 (from /speckit-analyze): removed product absent from search/list and lookup.
        assertThatThrownBy(() -> productService.getById(product.id()))
                .isInstanceOf(com.hubinity.catalog.api.error.ProductNotFoundException.class);
        var page = productService.search(categoryId, null, 0, 20, "name,asc");
        assertThat(page.content()).noneMatch(p -> p.id().equals(product.id()));

        // C2 (from /speckit-analyze): price history survives removal.
        var history = productService.getPriceHistory(product.id());
        assertThat(history).hasSize(1);
    }

    @Test
    @DisplayName("an ACTIVE reservation blocks removal even at zero available/reserved stock")
    void delete_blocked_by_active_reservation() {
        UUID categoryId = newCategory("delete-reservation-cat-" + UUID.randomUUID());
        ProductResponse product = productService.create(new ProductRequest(
                "DEL-RES-" + UUID.randomUUID(), "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null));

        StockReservation reservation = new StockReservation();
        reservation.setProductId(product.id());
        reservation.setQuantity(1);
        reservation.setStatus(StockReservationStatus.ACTIVE);
        reservation.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
        stockReservations.saveAndFlush(reservation);

        assertThatThrownBy(() -> productService.delete(product.id()))
                .isInstanceOf(com.hubinity.catalog.api.error.ProductHasStockOrReservationsException.class);
    }

    @Test
    @DisplayName("F1: a removal racing a newly placed reservation resolves consistently, never both succeeding incorrectly")
    void concurrent_delete_and_new_reservation_resolve_consistently() throws Exception {
        UUID categoryId = newCategory("delete-race-cat-" + UUID.randomUUID());
        ProductResponse product = productService.create(new ProductRequest(
                "DEL-RACE-" + UUID.randomUUID(), "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null));
        StockItem item = new StockItem();
        item.setProductId(product.id());
        item.setAvailable(0);
        stockItems.saveAndFlush(item);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Boolean> deleteResult = pool.submit(() -> {
            ready.countDown();
            go.await();
            try {
                productService.delete(product.id());
                return true;
            } catch (com.hubinity.catalog.api.error.ProductHasStockOrReservationsException e) {
                return false;
            }
        });
        Future<Boolean> reserveResult = pool.submit(() -> {
            ready.countDown();
            go.await();
            StockReservation reservation = new StockReservation();
            reservation.setProductId(product.id());
            reservation.setQuantity(1);
            reservation.setStatus(StockReservationStatus.ACTIVE);
            reservation.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
            stockReservations.saveAndFlush(reservation);
            return true;
        });

        ready.await();
        go.countDown();
        boolean deleted = deleteResult.get();
        reserveResult.get();
        pool.shutdown();

        boolean reservationExists = stockReservations.existsByProductIdAndStatus(product.id(), StockReservationStatus.ACTIVE);
        // Whichever operation's transaction committed first wins the lock; the other observes its
        // fully committed effect. The two outcomes are mutually exclusive — the product cannot be
        // both removed AND missing the reservation's blocking effect at the same time.
        if (deleted) {
            assertThat(reservationExists)
                    .as("if delete succeeded, no ACTIVE reservation should have been visible to it")
                    .isFalse();
        } else {
            assertThat(reservationExists)
                    .as("if delete was blocked, the reservation it saw must indeed be ACTIVE")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("a category with a linked product blocks category removal until the product itself is removed")
    void category_with_linked_product_blocks_removal_until_product_removed() {
        UUID categoryId = newCategory("delete-cat-products-" + UUID.randomUUID());
        ProductResponse product = productService.create(new ProductRequest(
                "DEL-CATPROD-" + UUID.randomUUID(), "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null));

        assertThatThrownBy(() -> categoryService.delete(categoryId))
                .isInstanceOf(com.hubinity.catalog.api.error.CategoryHasProductsException.class);

        productService.delete(product.id());

        categoryService.delete(categoryId);
    }
}

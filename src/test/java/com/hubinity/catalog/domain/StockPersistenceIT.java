package com.hubinity.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
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
import com.hubinity.catalog.api.dto.StockMovementRequest;
import com.hubinity.catalog.api.dto.StockReservationRequest;
import com.hubinity.catalog.domain.error.InsufficientStockException;
import com.hubinity.catalog.service.CategoryService;
import com.hubinity.catalog.service.ProductService;
import com.hubinity.catalog.service.StockService;

/**
 * Verifies the atomic reservation guarantee (FR-005, SC-001) and the expiry-vs-action race
 * guarantee (FR-020) against a real Postgres 16 container, plus the movement-history
 * cross-story surfacing check (T045c, closing {@code /speckit-analyze} finding E2). Mirrors
 * {@code ProductPersistenceIT} for container + property wiring.
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
@DisplayName("Stock movement & reservation persistence — integration")
class StockPersistenceIT {

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
    private CategoryService categoryService;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockItemRepository stockItems;

    @Autowired
    private StockReservationRepository stockReservations;

    private UUID newProduct(String prefix) {
        UUID categoryId = categoryService.create(new CategoryRequest(prefix, prefix, null, null, null)).id();
        ProductResponse product = productService.create(new ProductRequest(
                prefix + "-SKU", prefix, null, new BigDecimal("9.90"), null, categoryId, null, null));
        return product.id();
    }

    @Test
    @DisplayName("SC-001: N concurrent reservations together exceeding available stock — only the ones that fit succeed")
    void concurrent_reservations_never_oversell() throws Exception {
        UUID productId = newProduct("race-" + UUID.randomUUID());
        stockService.recordMovement(productId, new StockMovementRequest(StockMovementType.IN, 10, null));

        int attempts = 20;
        int qtyEach = 1;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                ready.countDown();
                go.await();
                try {
                    stockService.reserve(new StockReservationRequest(productId, qtyEach, null, Instant.now().plusSeconds(300)));
                    return true;
                } catch (InsufficientStockException e) {
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

        assertThat(successes).isEqualTo(10);
        StockItem finalState = stockItems.findById(productId).orElseThrow();
        assertThat(finalState.getAvailable()).isZero();
        assertThat(finalState.getReserved()).isEqualTo(10);
        assertThat(finalState.getAvailable() + finalState.getReserved()).isEqualTo(10);
    }

    @Test
    @DisplayName("US5: an overdue reservation is expired by the sweep, returning its quantity to available")
    void sweep_expires_overdue_reservation() {
        UUID productId = newProduct("sweep-" + UUID.randomUUID());
        stockService.recordMovement(productId, new StockMovementRequest(StockMovementType.IN, 10, null));
        var reserved = stockService.reserve(
                new StockReservationRequest(productId, 4, null, Instant.now().plusMillis(200)));
        UUID reservationId = reserved.reservation().id();

        await(250);
        stockService.sweepExpiredReservations();

        StockReservation reservation = stockReservations.findById(reservationId).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.EXPIRED);
        StockItem finalState = stockItems.findById(productId).orElseThrow();
        assertThat(finalState.getAvailable()).isEqualTo(10);
        assertThat(finalState.getReserved()).isZero();
    }

    @Test
    @DisplayName("FR-020: a sweep racing a concurrent commit on the same reservation resolves to exactly one outcome")
    void sweep_racing_commit_resolves_to_one_outcome() throws Exception {
        UUID productId = newProduct("sweeprace-" + UUID.randomUUID());
        stockService.recordMovement(productId, new StockMovementRequest(StockMovementType.IN, 10, null));
        var reserved = stockService.reserve(
                new StockReservationRequest(productId, 4, null, Instant.now().plusMillis(150)));
        UUID reservationId = reserved.reservation().id();
        await(200);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Boolean> commitResult = pool.submit(() -> {
            ready.countDown();
            go.await();
            try {
                stockService.commit(reservationId);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        });
        Future<Boolean> sweepResult = pool.submit(() -> {
            ready.countDown();
            go.await();
            stockService.sweepExpiredReservations();
            return true;
        });

        ready.await();
        go.countDown();
        boolean committed = commitResult.get();
        sweepResult.get();
        pool.shutdown();

        StockReservation finalReservation = stockReservations.findById(reservationId).orElseThrow();
        StockItem finalStock = stockItems.findById(productId).orElseThrow();
        if (committed) {
            assertThat(finalReservation.getStatus()).isEqualTo(StockReservationStatus.COMMITTED);
            assertThat(finalStock.getReserved()).isZero();
            assertThat(finalStock.getAvailable()).isEqualTo(6);
        } else {
            assertThat(finalReservation.getStatus()).isEqualTo(StockReservationStatus.EXPIRED);
            assertThat(finalStock.getReserved()).isZero();
            assertThat(finalStock.getAvailable()).isEqualTo(10);
        }
    }

    @Test
    @DisplayName("T045c (closes /speckit-analyze E2): every movement kind produced across reserve/commit/release/expire surfaces in history")
    void all_movement_kinds_surface_in_history() {
        UUID productId = newProduct("history-" + UUID.randomUUID());
        stockService.recordMovement(productId, new StockMovementRequest(StockMovementType.IN, 30, null));
        stockService.recordMovement(productId, new StockMovementRequest(StockMovementType.OUT, 2, null));

        var committed = stockService.reserve(new StockReservationRequest(productId, 5, null, Instant.now().plusSeconds(300)));
        stockService.commit(committed.reservation().id());

        var released = stockService.reserve(new StockReservationRequest(productId, 5, null, Instant.now().plusSeconds(300)));
        stockService.release(released.reservation().id());

        var expired = stockService.reserve(new StockReservationRequest(productId, 5, null, Instant.now().plusMillis(150)));
        await(200);
        stockService.sweepExpiredReservations();

        var history = stockService.getMovementHistory(productId, 0, 50);

        assertThat(history.content()).extracting(m -> m.type())
                .contains(StockMovementType.IN, StockMovementType.OUT, StockMovementType.RESERVE,
                        StockMovementType.COMMIT, StockMovementType.RELEASE);
        long releaseCount = history.content().stream().filter(m -> m.type() == StockMovementType.RELEASE).count();
        assertThat(releaseCount).isEqualTo(2); // one caller-driven release, one sweep-driven expiry
    }

    private void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

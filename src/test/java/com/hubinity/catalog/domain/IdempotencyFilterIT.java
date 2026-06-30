package com.hubinity.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.hubinity.catalog.api.dto.CategoryRequest;
import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.dto.StockMovementRequest;
import com.hubinity.catalog.api.dto.StockReservationRequest;
import com.hubinity.catalog.service.CategoryService;
import com.hubinity.catalog.service.ProductService;
import com.hubinity.catalog.service.StockService;

/**
 * Real end-to-end HTTP (via {@link MockMvc} against the full, non-sliced application context, so
 * the real {@code IdempotencyFilter} registered through {@code IdempotencyFilterConfig} actually
 * runs) verification across all four mutating stock endpoints — closes the gap
 * {@code /speckit-analyze} finding E1 raised (the original test plan only exercised the movement
 * endpoint, but SC-002/FR-014–016 claim the guarantee for "any movement, reservation, release, or
 * commit request").
 */
@SpringBootTest(properties = {"spring.security.oauth2.resourceserver.jwt.issuer-uri="})
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Testcontainers
@Tag("integration")
@DisplayName("IdempotencyFilter — integration")
class IdempotencyFilterIT {

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StockService stockService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));
    }

    private UUID newProduct(String prefix) {
        UUID categoryId = categoryService.create(new CategoryRequest(prefix, prefix, null, null, null)).id();
        return productService.create(new ProductRequest(
                prefix + "-SKU", prefix, null, new BigDecimal("9.90"), null, categoryId, null, null)).id();
    }

    private UUID newActiveReservation(UUID productId, int qty) {
        return stockService.reserve(new StockReservationRequest(productId, qty, null, Instant.now().plusSeconds(300)))
                .reservation().id();
    }

    @Test
    @DisplayName("movements: replay returns the original response verbatim, conflict on different body, 400 on missing header")
    void movements_idempotencyBehavior() throws Exception {
        UUID productId = newProduct("idem-mv-" + UUID.randomUUID());
        String body = "{\"type\":\"IN\",\"quantity\":10}";
        String differentBody = "{\"type\":\"IN\",\"quantity\":20}";
        String key = UUID.randomUUID().toString();

        MvcResult first = mockMvc.perform(post("/api/v1/products/{id}/stock/movements", productId).with(admin())
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        assertThat(first.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());

        MvcResult replay = mockMvc.perform(post("/api/v1/products/{id}/stock/movements", productId).with(admin())
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        assertThat(replay.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(replay.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());

        var history = stockService.getMovementHistory(productId, 0, 50);
        assertThat(history.content()).hasSize(1);

        MvcResult conflict = mockMvc.perform(post("/api/v1/products/{id}/stock/movements", productId).with(admin())
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(differentBody))
                .andReturn();
        assertThat(conflict.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(conflict.getResponse().getContentAsString()).contains("idempotency-key-conflict");

        MvcResult missingHeader = mockMvc.perform(post("/api/v1/products/{id}/stock/movements", productId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        assertThat(missingHeader.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(missingHeader.getResponse().getContentAsString()).contains("idempotency-key-missing");
    }

    @Test
    @DisplayName("reservations: replay returns the original response verbatim, conflict on different body, 400 on missing header")
    void reservations_idempotencyBehavior() throws Exception {
        UUID productId = newProduct("idem-res-" + UUID.randomUUID());
        stockService.recordMovement(productId, new StockMovementRequest(StockMovementType.IN, 100, null));
        String key = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(300);
        String body = "{\"productId\":\"" + productId + "\",\"quantity\":5,\"expiresAt\":\"" + expiresAt + "\"}";
        String differentBody = "{\"productId\":\"" + productId + "\",\"quantity\":9,\"expiresAt\":\"" + expiresAt + "\"}";

        MvcResult first = mockMvc.perform(post("/api/v1/stock/reservations").with(admin())
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        assertThat(first.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());

        MvcResult replay = mockMvc.perform(post("/api/v1/stock/reservations").with(admin())
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        assertThat(replay.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());

        MvcResult conflict = mockMvc.perform(post("/api/v1/stock/reservations").with(admin())
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(differentBody))
                .andReturn();
        assertThat(conflict.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());

        MvcResult missingHeader = mockMvc.perform(post("/api/v1/stock/reservations").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        assertThat(missingHeader.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("release: replay returns the original response verbatim, conflict when reused against a different reservation, 400 on missing header")
    void release_idempotencyBehavior() throws Exception {
        UUID productId = newProduct("idem-rel-" + UUID.randomUUID());
        stockService.recordMovement(productId, new StockMovementRequest(StockMovementType.IN, 100, null));
        UUID reservationA = newActiveReservation(productId, 3);
        UUID reservationB = newActiveReservation(productId, 3);
        String key = UUID.randomUUID().toString();

        MvcResult first = mockMvc.perform(post("/api/v1/stock/reservations/{id}/release", reservationA).with(admin())
                        .header("Idempotency-Key", key)).andReturn();
        assertThat(first.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());

        MvcResult replay = mockMvc.perform(post("/api/v1/stock/reservations/{id}/release", reservationA).with(admin())
                        .header("Idempotency-Key", key)).andReturn();
        assertThat(replay.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());

        // Same key reused against a DIFFERENT reservation (different path = different hash) = conflict.
        MvcResult conflict = mockMvc.perform(post("/api/v1/stock/reservations/{id}/release", reservationB).with(admin())
                        .header("Idempotency-Key", key)).andReturn();
        assertThat(conflict.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());

        MvcResult missingHeader = mockMvc.perform(post("/api/v1/stock/reservations/{id}/release", reservationB).with(admin()))
                .andReturn();
        assertThat(missingHeader.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("commit: replay returns the original response verbatim, 400 on missing header")
    void commit_idempotencyBehavior() throws Exception {
        UUID productId = newProduct("idem-com-" + UUID.randomUUID());
        stockService.recordMovement(productId, new StockMovementRequest(StockMovementType.IN, 100, null));
        UUID reservationId = newActiveReservation(productId, 3);
        String key = UUID.randomUUID().toString();

        MvcResult first = mockMvc.perform(post("/api/v1/stock/reservations/{id}/commit", reservationId).with(admin())
                        .header("Idempotency-Key", key)).andReturn();
        assertThat(first.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());

        MvcResult replay = mockMvc.perform(post("/api/v1/stock/reservations/{id}/commit", reservationId).with(admin())
                        .header("Idempotency-Key", key)).andReturn();
        assertThat(replay.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());

        MvcResult missingHeader = mockMvc.perform(post("/api/v1/stock/reservations/{id}/commit", reservationId).with(admin()))
                .andReturn();
        assertThat(missingHeader.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("two near-simultaneous requests with the same fresh key: exactly one success, one in-progress conflict")
    void concurrentDuplicateKey_resolvesToOneSuccessAndOneInProgress() throws Exception {
        UUID productId = newProduct("idem-race-" + UUID.randomUUID());
        String body = "{\"type\":\"IN\",\"quantity\":10}";
        String key = UUID.randomUUID().toString();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        var call = (java.util.concurrent.Callable<Integer>) () -> {
            ready.countDown();
            go.await();
            return mockMvc.perform(post("/api/v1/products/{id}/stock/movements", productId).with(admin())
                            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getStatus();
        };

        Future<Integer> f1 = pool.submit(call);
        Future<Integer> f2 = pool.submit(call);
        ready.await();
        go.countDown();

        int s1 = f1.get(10, TimeUnit.SECONDS);
        int s2 = f2.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        List<Integer> statuses = List.of(s1, s2);
        long successCount = statuses.stream().filter(s -> s == HttpStatus.CREATED.value()).count();
        long inProgressCount = statuses.stream().filter(s -> s == HttpStatus.CONFLICT.value()).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(inProgressCount).isEqualTo(1);

        var history = stockService.getMovementHistory(productId, 0, 50);
        assertThat(history.content()).hasSize(1);
    }
}

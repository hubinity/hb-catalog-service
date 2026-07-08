package com.hubinity.catalog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubinity.catalog.api.dto.StockItemResponse;
import com.hubinity.catalog.api.dto.StockMovementPageResponse;
import com.hubinity.catalog.api.dto.StockMovementRequest;
import com.hubinity.catalog.api.dto.StockMovementResponse;
import com.hubinity.catalog.api.dto.StockMovementResult;
import com.hubinity.catalog.api.dto.StockReservationRequest;
import com.hubinity.catalog.api.dto.StockReservationResponse;
import com.hubinity.catalog.api.dto.StockReservationResult;
import com.hubinity.catalog.api.error.InsufficientStockException;
import com.hubinity.catalog.api.error.ProductNotFoundException;
import com.hubinity.catalog.api.error.ReservationExpiredException;
import com.hubinity.catalog.api.error.ReservationNotActiveException;
import com.hubinity.catalog.api.error.ReservationNotFoundException;
import com.hubinity.catalog.config.SecurityConfig;
import com.hubinity.catalog.domain.StockMovementType;
import com.hubinity.catalog.domain.StockReservationStatus;
import com.hubinity.catalog.service.StockService;

/**
 * Web-layer slice tests for {@link StockController}. Loads {@link SecurityConfig} so the real
 * filter chain/role checks run; {@link StockService} is mocked to isolate the controller's
 * HTTP/validation/authorization behavior from business logic. {@code IdempotencyFilterConfig} is
 * intentionally not imported here — idempotency end-to-end behavior is covered separately by
 * {@code IdempotencyFilterIT} (Phase 8); this class focuses on the controller itself.
 */
@WebMvcTest(StockController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.profiles.active=",
        "app.security.keycloak.client-id=hb-catalog-service",
        "app.cors.allowed-origins=http://localhost:4200"
})
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private StockService stockService;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));
    }

    private static RequestPostProcessor nonAdmin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_tecnico"));
    }

    private String json(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private StockMovementResult movementResult(UUID productId, StockMovementType type, int qty, int available) {
        return new StockMovementResult(
                new StockMovementResponse(UUID.randomUUID(), productId, type, qty, null, Instant.now(), Instant.now(), null),
                new StockItemResponse(productId, available, 0, 0, Instant.now()));
    }

    private StockReservationResult reservationResult(UUID id, UUID productId, int qty, StockReservationStatus status) {
        return new StockReservationResult(
                new StockReservationResponse(id, productId, qty, null, status, Instant.now().plusSeconds(300), Instant.now()),
                new StockItemResponse(productId, 0, qty, 0, Instant.now()));
    }

    @Nested
    class RecordMovementEndpoint {

        @Test
        void validInMovement_withAdminRole_returns201() throws Exception {
            UUID productId = UUID.randomUUID();
            when(stockService.recordMovement(eq(productId), any())).thenReturn(movementResult(productId, StockMovementType.IN, 10, 10));

            mockMvc.perform(post("/api/v1/products/{id}/stock/movements", productId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockMovementRequest(StockMovementType.IN, 10, null))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.stock.available").value(10));
        }

        @Test
        void zeroQuantity_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/products/{id}/stock/movements", UUID.randomUUID()).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockMovementRequest(StockMovementType.IN, 0, null))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unknownProduct_returns404() throws Exception {
            UUID productId = UUID.randomUUID();
            when(stockService.recordMovement(eq(productId), any())).thenThrow(new ProductNotFoundException(productId));

            mockMvc.perform(post("/api/v1/products/{id}/stock/movements", productId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockMovementRequest(StockMovementType.IN, 1, null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:product-not-found"));
        }

        @Test
        void insufficientStock_returns409() throws Exception {
            UUID productId = UUID.randomUUID();
            when(stockService.recordMovement(eq(productId), any()))
                    .thenThrow(new InsufficientStockException(productId, 1000, 5));

            mockMvc.perform(post("/api/v1/products/{id}/stock/movements", productId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockMovementRequest(StockMovementType.OUT, 1000, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:insufficient-stock"));
        }

        @Test
        void withoutAdminRole_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/products/{id}/stock/movements", UUID.randomUUID()).with(nonAdmin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockMovementRequest(StockMovementType.IN, 1, null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/products/{id}/stock/movements", UUID.randomUUID())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockMovementRequest(StockMovementType.IN, 1, null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetMovementHistoryEndpoint {

        @Test
        void anyAuthenticatedRole_returns200() throws Exception {
            UUID productId = UUID.randomUUID();
            when(stockService.getMovementHistory(eq(productId), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenReturn(new StockMovementPageResponse(java.util.List.of(
                            new StockMovementResponse(UUID.randomUUID(), productId, StockMovementType.IN, 10, null, Instant.now(), Instant.now(), null)),
                            0, 20, 1, 1));

            mockMvc.perform(get("/api/v1/products/{id}/stock/movements", productId).with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].type").value("IN"));
        }

        @Test
        void unknownProduct_returns404() throws Exception {
            UUID productId = UUID.randomUUID();
            when(stockService.getMovementHistory(eq(productId), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenThrow(new ProductNotFoundException(productId));

            mockMvc.perform(get("/api/v1/products/{id}/stock/movements", productId).with(admin()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ReserveEndpoint {

        @Test
        void validRequest_withAdminRole_returns201() throws Exception {
            UUID productId = UUID.randomUUID();
            UUID reservationId = UUID.randomUUID();
            when(stockService.reserve(any())).thenReturn(reservationResult(reservationId, productId, 5, StockReservationStatus.ACTIVE));

            mockMvc.perform(post("/api/v1/stock/reservations").with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockReservationRequest(productId, 5, null, Instant.now().plusSeconds(300)))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reservation.status").value("ACTIVE"));
        }

        @Test
        void pastExpiresAt_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/stock/reservations").with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockReservationRequest(UUID.randomUUID(), 5, null, Instant.now().minusSeconds(300)))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unknownProduct_returns404() throws Exception {
            UUID productId = UUID.randomUUID();
            when(stockService.reserve(any())).thenThrow(new ProductNotFoundException(productId));

            mockMvc.perform(post("/api/v1/stock/reservations").with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockReservationRequest(productId, 5, null, Instant.now().plusSeconds(300)))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void insufficientStock_returns409() throws Exception {
            UUID productId = UUID.randomUUID();
            when(stockService.reserve(any())).thenThrow(new InsufficientStockException(productId, 1000, 5));

            mockMvc.perform(post("/api/v1/stock/reservations").with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new StockReservationRequest(productId, 1000, null, Instant.now().plusSeconds(300)))))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class CommitEndpoint {

        @Test
        void activeReservation_withAdminRole_returns200() throws Exception {
            UUID reservationId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            when(stockService.commit(reservationId)).thenReturn(reservationResult(reservationId, productId, 5, StockReservationStatus.COMMITTED));

            mockMvc.perform(post("/api/v1/stock/reservations/{id}/commit", reservationId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reservation.status").value("COMMITTED"));
        }

        @Test
        void unknownReservation_returns404() throws Exception {
            UUID reservationId = UUID.randomUUID();
            when(stockService.commit(reservationId)).thenThrow(new ReservationNotFoundException(reservationId));

            mockMvc.perform(post("/api/v1/stock/reservations/{id}/commit", reservationId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void notActiveReservation_returns409() throws Exception {
            UUID reservationId = UUID.randomUUID();
            when(stockService.commit(reservationId))
                    .thenThrow(new ReservationNotActiveException(reservationId, StockReservationStatus.RELEASED));

            mockMvc.perform(post("/api/v1/stock/reservations/{id}/commit", reservationId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:reservation-not-active"));
        }

        @Test
        void expiredReservation_returns409() throws Exception {
            UUID reservationId = UUID.randomUUID();
            when(stockService.commit(reservationId)).thenThrow(new ReservationExpiredException(reservationId));

            mockMvc.perform(post("/api/v1/stock/reservations/{id}/commit", reservationId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:reservation-expired"));
        }
    }

    @Nested
    class ReleaseEndpoint {

        @Test
        void activeReservation_withAdminRole_returns200() throws Exception {
            UUID reservationId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            when(stockService.release(reservationId)).thenReturn(reservationResult(reservationId, productId, 5, StockReservationStatus.RELEASED));

            mockMvc.perform(post("/api/v1/stock/reservations/{id}/release", reservationId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reservation.status").value("RELEASED"));
        }

        @Test
        void unknownReservation_returns404() throws Exception {
            UUID reservationId = UUID.randomUUID();
            when(stockService.release(reservationId)).thenThrow(new ReservationNotFoundException(reservationId));

            mockMvc.perform(post("/api/v1/stock/reservations/{id}/release", reservationId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void notActiveReservation_returns409() throws Exception {
            UUID reservationId = UUID.randomUUID();
            when(stockService.release(reservationId))
                    .thenThrow(new ReservationNotActiveException(reservationId, StockReservationStatus.COMMITTED));

            mockMvc.perform(post("/api/v1/stock/reservations/{id}/release", reservationId).with(admin())
                            .header("Idempotency-Key", UUID.randomUUID().toString()))
                    .andExpect(status().isConflict());
        }
    }
}

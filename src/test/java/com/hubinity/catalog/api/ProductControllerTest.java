package com.hubinity.catalog.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.hubinity.catalog.api.dto.ProductPageResponse;
import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.dto.ProductResponse;
import com.hubinity.catalog.api.error.DuplicateSkuException;
import com.hubinity.catalog.api.error.InvalidCategoryException;
import com.hubinity.catalog.api.dto.PriceHistoryResponse;
import com.hubinity.catalog.api.error.InvalidPaginationException;
import com.hubinity.catalog.api.error.ProductHasStockOrReservationsException;
import com.hubinity.catalog.api.error.ProductNotFoundException;
import com.hubinity.catalog.config.SecurityConfig;
import com.hubinity.catalog.service.ProductService;

/**
 * Web-layer slice tests for {@link ProductController}. Loads {@link SecurityConfig} so the
 * real filter chain/role checks run; {@link ProductService} is mocked to isolate the
 * controller's HTTP/validation/authorization behavior from business logic.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.profiles.active=",
        "app.security.keycloak.client-id=hb-catalog-service",
        "app.cors.allowed-origins=http://localhost:4200"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ProductService productService;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));
    }

    private static RequestPostProcessor nonAdmin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_tecnico"));
    }

    private String json(ProductRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private ProductRequest validRequest(UUID categoryId) {
        return new ProductRequest("SKU-001", "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null);
    }

    @Nested
    class CreateEndpoint {

        @Test
        void validRequest_withAdminRole_returns201WithLocationAndBody() throws Exception {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            ProductResponse response = new ProductResponse(
                    id, "SKU-001", "Widget", null, new BigDecimal("9.90"), null, categoryId, true, null,
                    Instant.now(), Instant.now());
            when(productService.create(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/products").with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/products/" + id)))
                    .andExpect(jsonPath("$.sku").value("SKU-001"));
        }

        @Test
        void blankSku_returns400() throws Exception {
            UUID categoryId = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/products").with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new ProductRequest("", "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.sku").exists());
        }

        @Test
        void duplicateSku_returns409() throws Exception {
            UUID categoryId = UUID.randomUUID();
            when(productService.create(any())).thenThrow(new DuplicateSkuException("SKU-001"));

            mockMvc.perform(post("/api/v1/products").with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:duplicate-sku"));
        }

        @Test
        void invalidCategory_returns422() throws Exception {
            UUID categoryId = UUID.randomUUID();
            when(productService.create(any())).thenThrow(new InvalidCategoryException(categoryId));

            mockMvc.perform(post("/api/v1/products").with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:invalid-category"));
        }

        @Test
        void withoutAdminRole_returns403() throws Exception {
            UUID categoryId = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/products").with(nonAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            UUID categoryId = UUID.randomUUID();
            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetByIdEndpoint {

        @Test
        void found_anyAuthenticatedRole_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            ProductResponse response = new ProductResponse(
                    id, "SKU-001", "Widget", null, new BigDecimal("9.90"), null, UUID.randomUUID(), true, null,
                    Instant.now(), Instant.now());
            when(productService.getById(id)).thenReturn(response);

            mockMvc.perform(get("/api/v1/products/{id}", id).with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value("SKU-001"));
        }

        @Test
        void unknown_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getById(id)).thenThrow(new ProductNotFoundException(id));

            mockMvc.perform(get("/api/v1/products/{id}", id).with(admin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:product-not-found"));
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class SearchEndpoint {

        @Test
        void anyAuthenticatedRole_returnsPageShape() throws Exception {
            ProductResponse response = new ProductResponse(
                    UUID.randomUUID(), "SKU-001", "Widget", null, new BigDecimal("9.90"), null,
                    UUID.randomUUID(), true, null, Instant.now(), Instant.now());
            when(productService.search(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt(), any()))
                    .thenReturn(new ProductPageResponse(java.util.List.of(response), 0, 20, 1, 1));

            mockMvc.perform(get("/api/v1/products").with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].sku").value("SKU-001"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void invalidPageSize_returns400() throws Exception {
            when(productService.search(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.eq(200), any()))
                    .thenThrow(new InvalidPaginationException("page size must be between 1 and 100"));

            mockMvc.perform(get("/api/v1/products").param("size", "200").with(nonAdmin()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:invalid-pagination"));
        }

        @Test
        void zeroPageSize_returns400() throws Exception {
            when(productService.search(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.eq(0), any()))
                    .thenThrow(new InvalidPaginationException("page size must be between 1 and 100"));

            mockMvc.perform(get("/api/v1/products").param("size", "0").with(nonAdmin()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unsupportedSortField_returns400() throws Exception {
            when(productService.search(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq("unknownField,asc")))
                    .thenThrow(new InvalidPaginationException("sort field not supported: unknownField"));

            mockMvc.perform(get("/api/v1/products").param("sort", "unknownField,asc").with(nonAdmin()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UpdateEndpoint {

        @Test
        void validRequest_withAdminRole_returns200WithUpdatedBody() throws Exception {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            ProductResponse response = new ProductResponse(
                    id, "SKU-001", "Renamed", null, new BigDecimal("9.90"), null, categoryId, true, null,
                    Instant.now(), Instant.now());
            when(productService.update(eq(id), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/products/{id}", id).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Renamed"));
        }

        @Test
        void blankSku_returns400() throws Exception {
            UUID categoryId = UUID.randomUUID();
            mockMvc.perform(put("/api/v1/products/{id}", UUID.randomUUID()).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new ProductRequest("", "Widget", null, new BigDecimal("9.90"), null, categoryId, null, null))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unknownId_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            when(productService.update(eq(id), any())).thenThrow(new ProductNotFoundException(id));

            mockMvc.perform(put("/api/v1/products/{id}", id).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void duplicateSku_returns409() throws Exception {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            when(productService.update(eq(id), any())).thenThrow(new DuplicateSkuException("SKU-001"));

            mockMvc.perform(put("/api/v1/products/{id}", id).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isConflict());
        }

        @Test
        void invalidCategory_returns422() throws Exception {
            UUID id = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            when(productService.update(eq(id), any())).thenThrow(new InvalidCategoryException(categoryId));

            mockMvc.perform(put("/api/v1/products/{id}", id).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void withoutAdminRole_returns403() throws Exception {
            UUID categoryId = UUID.randomUUID();
            mockMvc.perform(put("/api/v1/products/{id}", UUID.randomUUID()).with(nonAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest(categoryId))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class PriceHistoryEndpoint {

        @Test
        void anyAuthenticatedRole_returns200Array() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getPriceHistory(id)).thenReturn(java.util.List.of(
                    new PriceHistoryResponse(UUID.randomUUID(), id, new BigDecimal("9.90"), Instant.now())));

            mockMvc.perform(get("/api/v1/products/{id}/price-history", id).with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].price").value(9.90));
        }

        @Test
        void unknownId_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getPriceHistory(id)).thenThrow(new ProductNotFoundException(id));

            mockMvc.perform(get("/api/v1/products/{id}/price-history", id).with(admin()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteEndpoint {

        @Test
        void noStockOrReservations_withAdminRole_returns204() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/products/{id}", id).with(admin()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void unknownId_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new ProductNotFoundException(id)).when(productService).delete(id);

            mockMvc.perform(delete("/api/v1/products/{id}", id).with(admin()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void hasStockOrReservations_returns409() throws Exception {
            UUID id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new ProductHasStockOrReservationsException(id)).when(productService).delete(id);

            mockMvc.perform(delete("/api/v1/products/{id}", id).with(admin()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:product-has-stock-or-reservations"));
        }

        @Test
        void withoutAdminRole_returns403() throws Exception {
            mockMvc.perform(delete("/api/v1/products/{id}", UUID.randomUUID()).with(nonAdmin()))
                    .andExpect(status().isForbidden());
        }
    }
}

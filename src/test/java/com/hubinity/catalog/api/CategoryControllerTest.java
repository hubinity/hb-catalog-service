package com.hubinity.catalog.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import com.hubinity.catalog.api.dto.CategoryRequest;
import com.hubinity.catalog.api.dto.CategoryResponse;
import com.hubinity.catalog.api.dto.CategoryTreeNode;
import com.hubinity.catalog.api.error.CategoryHasChildrenException;
import com.hubinity.catalog.api.error.CategoryHasProductsException;
import com.hubinity.catalog.api.error.CategoryNotFoundException;
import com.hubinity.catalog.api.error.CircularReferenceException;
import com.hubinity.catalog.api.error.DuplicateSlugException;
import com.hubinity.catalog.api.error.InvalidParentException;
import com.hubinity.catalog.config.SecurityConfig;
import com.hubinity.catalog.service.CategoryService;

/**
 * Web-layer slice tests for {@link CategoryController}. Loads {@link SecurityConfig} so the
 * real filter chain/role checks run; {@link CategoryService} is mocked to isolate the
 * controller's HTTP/validation/authorization behavior from business logic.
 */
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.profiles.active=",
        "app.security.keycloak.client-id=hb-catalog-service",
        "app.cors.allowed-origins=http://localhost:4200"
})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CategoryService categoryService;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));
    }

    private static RequestPostProcessor nonAdmin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_tecnico"));
    }

    private String json(CategoryRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Nested
    class CreateEndpoint {

        @Test
        void validRequest_withAdminRole_returns201WithLocationAndBody() throws Exception {
            UUID id = UUID.randomUUID();
            CategoryResponse response = new CategoryResponse(
                    id, "Electronics", "electronics", null, 0, true, Instant.now(), Instant.now());
            when(categoryService.create(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/categories").with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Electronics", "electronics", null, null, null))))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/categories/" + id)))
                    .andExpect(jsonPath("$.slug").value("electronics"));
        }

        @Test
        void blankName_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/categories").with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("", "electronics", null, null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        void duplicateSlug_returns409() throws Exception {
            when(categoryService.create(any())).thenThrow(new DuplicateSlugException("electronics"));

            mockMvc.perform(post("/api/v1/categories").with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Electronics", "electronics", null, null, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:duplicate-slug"));
        }

        @Test
        void invalidParent_returns422() throws Exception {
            when(categoryService.create(any())).thenThrow(new InvalidParentException(UUID.randomUUID()));

            mockMvc.perform(post("/api/v1/categories").with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Phones", "phones", UUID.randomUUID(), null, null))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:invalid-parent"));
        }

        @Test
        void withoutAdminRole_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/categories").with(nonAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Electronics", "electronics", null, null, null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Electronics", "electronics", null, null, null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetByIdEndpoint {

        @Test
        void found_anyAuthenticatedRole_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            CategoryResponse response = new CategoryResponse(
                    id, "Electronics", "electronics", null, 0, true, Instant.now(), Instant.now());
            when(categoryService.getById(id)).thenReturn(response);

            mockMvc.perform(get("/api/v1/categories/{id}", id).with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("electronics"));
        }

        @Test
        void unknown_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryService.getById(id)).thenThrow(new CategoryNotFoundException(id));

            mockMvc.perform(get("/api/v1/categories/{id}", id).with(admin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:category-not-found"));
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/categories/{id}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class ListEndpoint {

        @Test
        void flat_anyAuthenticatedRole_returnsFlatArray() throws Exception {
            CategoryResponse response = new CategoryResponse(
                    UUID.randomUUID(), "Electronics", "electronics", null, 0, true, Instant.now(), Instant.now());
            when(categoryService.listFlat()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/categories").with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].slug").value("electronics"));
        }

        @Test
        void flat_empty_returnsEmptyArray() throws Exception {
            when(categoryService.listFlat()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/categories").with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void tree_anyAuthenticatedRole_returnsNestedArray() throws Exception {
            CategoryTreeNode child = new CategoryTreeNode(UUID.randomUUID(), "Phones", "phones", 0, true, List.of());
            CategoryTreeNode root = new CategoryTreeNode(
                    UUID.randomUUID(), "Electronics", "electronics", 0, true, List.of(child));
            when(categoryService.listTree()).thenReturn(List.of(root));

            mockMvc.perform(get("/api/v1/categories").param("tree", "true").with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].children[0].slug").value("phones"));
        }

        @Test
        void tree_empty_returnsEmptyArray() throws Exception {
            when(categoryService.listTree()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/categories").param("tree", "true").with(nonAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UpdateEndpoint {

        @Test
        void validRequest_withAdminRole_returns200WithUpdatedBody() throws Exception {
            UUID id = UUID.randomUUID();
            CategoryResponse response = new CategoryResponse(
                    id, "Renamed", "renamed", null, 0, true, Instant.now(), Instant.now());
            when(categoryService.update(eq(id), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/categories/{id}", id).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Renamed", "renamed", null, null, null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Renamed"));
        }

        @Test
        void blankName_returns400() throws Exception {
            mockMvc.perform(put("/api/v1/categories/{id}", UUID.randomUUID()).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("", "electronics", null, null, null))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unknownId_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryService.update(eq(id), any())).thenThrow(new CategoryNotFoundException(id));

            mockMvc.perform(put("/api/v1/categories/{id}", id).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Electronics", "electronics", null, null, null))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void duplicateSlug_returns409() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryService.update(eq(id), any())).thenThrow(new DuplicateSlugException("electronics"));

            mockMvc.perform(put("/api/v1/categories/{id}", id).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Electronics", "electronics", null, null, null))))
                    .andExpect(status().isConflict());
        }

        @Test
        void circularReference_returns422() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryService.update(eq(id), any())).thenThrow(new CircularReferenceException(id, id));

            mockMvc.perform(put("/api/v1/categories/{id}", id).with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Electronics", "electronics", id, null, null))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:circular-reference"));
        }

        @Test
        void withoutAdminRole_returns403() throws Exception {
            mockMvc.perform(put("/api/v1/categories/{id}", UUID.randomUUID()).with(nonAdmin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new CategoryRequest("Electronics", "electronics", null, null, null))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class DeleteEndpoint {

        @Test
        void childless_withAdminRole_returns204() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/categories/{id}", id).with(admin()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void unknownId_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new CategoryNotFoundException(id)).when(categoryService).delete(id);

            mockMvc.perform(delete("/api/v1/categories/{id}", id).with(admin()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void hasChildren_returns409() throws Exception {
            UUID id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new CategoryHasChildrenException(id)).when(categoryService).delete(id);

            mockMvc.perform(delete("/api/v1/categories/{id}", id).with(admin()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:category-has-children"));
        }

        @Test
        void hasLinkedProducts_returns409() throws Exception {
            UUID id = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new CategoryHasProductsException(id)).when(categoryService).delete(id);

            mockMvc.perform(delete("/api/v1/categories/{id}", id).with(admin()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:hubinity:catalog:category-has-products"));
        }

        @Test
        void withoutAdminRole_returns403() throws Exception {
            mockMvc.perform(delete("/api/v1/categories/{id}", UUID.randomUUID()).with(nonAdmin()))
                    .andExpect(status().isForbidden());
        }
    }
}

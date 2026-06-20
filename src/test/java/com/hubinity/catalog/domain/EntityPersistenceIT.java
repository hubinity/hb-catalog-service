package com.hubinity.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

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

/**
 * Verifies the JPA entities + repositories against a real Postgres 16 container.
 *
 * <p>Tagged {@code integration} so default {@code mvn test} skips it. Uses the
 * {@code integration} profile (NOT {@code test}) so {@link com.hubinity.catalog.config.JpaAuditingConfig}
 * (gated by {@code @Profile("!test")}) DOES activate; this is required to
 * populate {@code @CreatedBy}/{@code @LastModifiedBy} via the auditor bean.
 * Without an authenticated principal the auditor returns {@code "system"} —
 * that's the expected value for {@code createdBy}/{@code updatedBy} here.
 *
 * <p>Mirrors {@code FlywayMigrationIT} for container + property wiring.
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
@DisplayName("Entity persistence + auditing — integration")
class EntityPersistenceIT {

    @Container
    @SuppressWarnings("resource") // lifecycle managed by @Testcontainers
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
    private CategoryRepository categories;

    @Autowired
    private ProductRepository products;

    @Test
    @DisplayName("Category save assigns a UUID and populates audit columns")
    void category_save_assigns_uuid_and_audit() {
        Category c = new Category();
        c.setName("Drinks");
        c.setSlug("drinks-" + UUID.randomUUID());

        Category saved = categories.saveAndFlush(c);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("system");
        assertThat(saved.getUpdatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("Product save assigns a UUID and system auditor")
    void product_save_assigns_uuid_and_audit_with_system_auditor() {
        Category cat = new Category();
        cat.setName("Snacks");
        cat.setSlug("snacks-" + UUID.randomUUID());
        cat = categories.saveAndFlush(cat);

        Product p = new Product();
        p.setSku("SKU-" + UUID.randomUUID());
        p.setName("Bag of chips");
        p.setPrice(new BigDecimal("4.50"));
        p.setCategoryId(cat.getId());

        Product saved = products.saveAndFlush(p);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("system");
        assertThat(saved.getUpdatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("Soft delete excludes product from findAll via @SQLRestriction")
    void soft_delete_excludes_product_from_findAll() {
        Category cat = new Category();
        cat.setName("Cookies");
        cat.setSlug("cookies-" + UUID.randomUUID());
        cat = categories.saveAndFlush(cat);

        Product p = new Product();
        p.setSku("SKU-" + UUID.randomUUID());
        p.setName("Choco chip");
        p.setPrice(new BigDecimal("3.00"));
        p.setCategoryId(cat.getId());
        Product saved = products.saveAndFlush(p);

        long before = products.count();
        saved.softDelete();
        products.saveAndFlush(saved);

        assertThat(products.count()).isEqualTo(before - 1);
        assertThat(products.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Category findBySlug returns the persisted row")
    void category_findBySlug_returns_persisted() {
        String slug = "find-me-" + UUID.randomUUID();
        Category c = new Category();
        c.setName("Find me");
        c.setSlug(slug);
        categories.saveAndFlush(c);

        Optional<Category> found = categories.findBySlug(slug);

        assertThat(found).isPresent();
        assertThat(found.get().getSlug()).isEqualTo(slug);
    }
}

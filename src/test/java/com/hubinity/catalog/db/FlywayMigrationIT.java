package com.hubinity.catalog.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the V1 Flyway migration against a real Postgres 16 container.
 *
 * <p>Tagged {@code integration} so the default {@code mvn test} run skips it
 * (Surefire excludes the tag). Run via the {@code integration-tests} profile:
 * {@code mvn -P integration-tests verify}. Requires a working Docker daemon.</p>
 *
 * <p>See PRD §4.1 for the entities under test and
 * {@code docs/adr/0011-soft-delete-deleted-at.md} for the soft-delete
 * convention these assertions exercise.</p>
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                // The default app YAML enables ddl-auto=validate, which would
                // refuse to start because no @Entity classes exist yet
                // (entities land in task 1.4). Disable validation here — we
                // only care that Flyway ran the migration.
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
// We intentionally do NOT pick up the "local" profile here — its Swagger
// SecurityFilterChain bean conflicts with the default one at startup. The
// "integration" profile is otherwise empty and just steers Spring away from
// the "local"-only beans.
@ActiveProfiles("integration")
@Testcontainers
@Tag("integration")
@DisplayName("Flyway V1 migration — integration")
class FlywayMigrationIT {

    private static final Pattern UUID_V7 = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    );

    @Container
    @SuppressWarnings("resource") // lifecycle is managed by @Testcontainers
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

    /**
     * Security config still wires the OAuth2 resource server, which expects a
     * {@code JwtDecoder} bean even though we override the issuer-uri to empty.
     * Mock it so the context can start without contacting Keycloak.
     */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway migrates V1 cleanly — all five tables exist")
    void flyway_migrates_cleanly() {
        Integer tableCount = jdbc.queryForObject(
                """
                SELECT count(*)::int FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('category', 'product', 'stock_item',
                                     'stock_movement', 'stock_reservation')
                """,
                Integer.class);

        assertThat(tableCount)
                .as("all five catalog tables should be created by V1")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("uuidv7() returns a valid RFC 9562 v7 UUID")
    void uuidv7_function_returns_valid_v7_uuid() {
        String uuid = jdbc.queryForObject("SELECT uuidv7()::text", String.class);

        assertThat(uuid).isNotNull();
        assertThat(uuid)
                .as("uuidv7() output must match the RFC 9562 v7 format")
                .matches(UUID_V7);

        // Also confirm Postgres considers the value a valid UUID type.
        assertThatCode(() -> UUID.fromString(uuid)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Partial unique allows re-using a SKU after soft-delete")
    void partial_unique_index_allows_reused_sku_after_soft_delete() {
        // Parent category — products require a category_id (FK, NOT NULL).
        UUID categoryId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO category (id, name, slug)
                VALUES (?, 'Drinks', 'drinks-' || ?)
                """,
                categoryId, categoryId.toString());

        // First product with sku 'ABC123' — succeeds.
        UUID firstProductId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO product (id, sku, name, price, category_id)
                VALUES (?, 'ABC123', 'Latte v1', 9.50, ?)
                """,
                firstProductId, categoryId);

        // Soft-delete it.
        jdbc.update("UPDATE product SET deleted_at = NOW() WHERE id = ?", firstProductId);

        // Second INSERT re-using the SKU must succeed because the partial
        // unique index filters out deleted_at IS NOT NULL rows.
        UUID secondProductId = UUID.randomUUID();
        assertThatCode(() -> jdbc.update(
                """
                INSERT INTO product (id, sku, name, price, category_id)
                VALUES (?, 'ABC123', 'Latte v2', 10.00, ?)
                """,
                secondProductId, categoryId))
                .as("re-inserting a SKU after soft-delete must not raise a unique-violation")
                .doesNotThrowAnyException();

        // Sanity: both rows now exist.
        Integer count = jdbc.queryForObject(
                "SELECT count(*)::int FROM product WHERE sku = 'ABC123'",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("set_updated_at trigger bumps updated_at on UPDATE")
    void set_updated_at_trigger_bumps_timestamp() throws InterruptedException {
        UUID categoryId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO category (id, name, slug)
                VALUES (?, 'Snacks', 'snacks-' || ?)
                """,
                categoryId, categoryId.toString());

        OffsetDateTime before = jdbc.queryForObject(
                "SELECT updated_at FROM category WHERE id = ?",
                OffsetDateTime.class, categoryId);

        // Force at least one clock_timestamp() tick before the UPDATE.
        Thread.sleep(50L);

        jdbc.update("UPDATE category SET name = 'Snacks (renamed)' WHERE id = ?", categoryId);

        OffsetDateTime after = jdbc.queryForObject(
                "SELECT updated_at FROM category WHERE id = ?",
                OffsetDateTime.class, categoryId);

        assertThat(before).isNotNull();
        assertThat(after).isNotNull();
        assertThat(after)
                .as("trigger trg_category_set_updated_at must bump updated_at on UPDATE")
                .isAfter(before);
    }
}

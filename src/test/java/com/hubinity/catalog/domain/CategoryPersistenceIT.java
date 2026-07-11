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
import com.hubinity.catalog.api.dto.CategoryResponse;
import com.hubinity.catalog.api.dto.CategoryTreeNode;
import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.dto.ProductResponse;
import com.hubinity.catalog.domain.error.CategoryHasChildrenException;
import com.hubinity.catalog.domain.error.CircularReferenceException;
import com.hubinity.catalog.service.CategoryService;
import com.hubinity.catalog.service.ProductService;

/**
 * Verifies {@code Category} CRUD persistence against a real Postgres 16 container,
 * including the partial unique slug index's behavior under concurrent writes.
 * Mirrors {@code EntityPersistenceIT} for container + property wiring.
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
@DisplayName("Category persistence — integration")
class CategoryPersistenceIT {

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
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private ProductRepository products;

    @Test
    @DisplayName("create then fetch by id returns every submitted field")
    void create_and_fetch_roundtrip_persists_all_fields() {
        String slug = "roundtrip-" + UUID.randomUUID();
        CategoryRequest req = new CategoryRequest("Roundtrip", slug, null, 5, false);

        CategoryResponse created = categoryService.create(req);
        CategoryResponse fetched = categoryService.getById(created.id());

        assertThat(fetched.name()).isEqualTo("Roundtrip");
        assertThat(fetched.slug()).isEqualTo(slug);
        assertThat(fetched.parentId()).isNull();
        assertThat(fetched.displayOrder()).isEqualTo(5);
        assertThat(fetched.active()).isFalse();
    }

    @Test
    @DisplayName("two concurrent inserts with the same slug: exactly one succeeds at the DB constraint level")
    void concurrent_inserts_same_slug_only_one_succeeds() throws Exception {
        String slug = "concurrent-" + UUID.randomUUID();
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
                    Category c = new Category();
                    c.setName("Race");
                    c.setSlug(slug);
                    categories.saveAndFlush(c);
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
        assertThat(categories.findBySlug(slug)).isPresent();
    }

    @Test
    @DisplayName("a 5+ level chain nests correctly in the tree view, with no artificial depth cap")
    void deep_chain_nests_correctly_in_tree() {
        String prefix = "deep-" + UUID.randomUUID() + "-";
        CategoryResponse root = categoryService.create(new CategoryRequest("Root", prefix + "root", null, null, null));
        CategoryResponse child = categoryService.create(
                new CategoryRequest("Child", prefix + "child", root.id(), null, null));
        CategoryResponse grandchild = categoryService.create(
                new CategoryRequest("Grandchild", prefix + "grandchild", child.id(), null, null));
        CategoryResponse great = categoryService.create(
                new CategoryRequest("Great", prefix + "great", grandchild.id(), null, null));
        categoryService.create(new CategoryRequest("GreatGreat", prefix + "greatgreat", great.id(), null, null));

        List<CategoryTreeNode> tree = categoryService.listTree();

        CategoryTreeNode node = tree.stream().filter(n -> n.id().equals(root.id())).findFirst().orElseThrow();
        for (int level = 0; level < 4; level++) {
            assertThat(node.children()).hasSize(1);
            node = node.children().get(0);
        }
        assertThat(node.name()).isEqualTo("GreatGreat");
    }

    @Test
    @DisplayName("update persists across a fresh read")
    void update_persists_across_fresh_read() {
        String slug = "updatable-" + UUID.randomUUID();
        CategoryResponse created = categoryService.create(new CategoryRequest("Original", slug, null, null, null));

        categoryService.update(created.id(), new CategoryRequest("Renamed", slug, null, 3, false));
        CategoryResponse fetched = categoryService.getById(created.id());

        assertThat(fetched.name()).isEqualTo("Renamed");
        assertThat(fetched.displayOrder()).isEqualTo(3);
        assertThat(fetched.active()).isFalse();
    }

    @Test
    @DisplayName("a circular-parent update is rejected and the tree is unchanged afterward")
    void circular_parent_update_rejected_tree_unchanged() {
        String prefix = "cycle-" + UUID.randomUUID() + "-";
        CategoryResponse root = categoryService.create(new CategoryRequest("Root", prefix + "root", null, null, null));
        CategoryResponse child = categoryService.create(
                new CategoryRequest("Child", prefix + "child", root.id(), null, null));

        assertThatThrownBy(() -> categoryService.update(
                root.id(), new CategoryRequest("Root", prefix + "root", child.id(), null, null)))
                .isInstanceOf(CircularReferenceException.class);

        List<CategoryTreeNode> tree = categoryService.listTree();
        CategoryTreeNode rootNode = tree.stream().filter(n -> n.id().equals(root.id())).findFirst().orElseThrow();
        assertThat(rootNode.children()).hasSize(1);
        assertThat(rootNode.children().get(0).id()).isEqualTo(child.id());
    }

    @Test
    @DisplayName("a removed category is absent from both the flat list and the tree")
    void removed_category_absent_from_list_and_tree() {
        String slug = "removable-" + UUID.randomUUID();
        Category c = new Category();
        c.setName("Removable");
        c.setSlug(slug);
        c = categories.saveAndFlush(c);
        UUID id = c.getId();

        c.softDelete();
        categories.saveAndFlush(c);

        assertThat(categories.findById(id)).isEmpty();
        assertThat(categoryService.listFlat()).noneMatch(r -> r.id().equals(id));
        assertThat(categoryService.listTree()).noneMatch(n -> n.id().equals(id));
    }

    @Test
    @DisplayName("delete is blocked while a child exists, then succeeds once the child is removed")
    void delete_blocked_with_child_then_succeeds_after_child_removed() {
        String prefix = "delete-" + UUID.randomUUID() + "-";
        CategoryResponse parent = categoryService.create(new CategoryRequest("Parent", prefix + "parent", null, null, null));
        CategoryResponse child = categoryService.create(
                new CategoryRequest("Child", prefix + "child", parent.id(), null, null));

        assertThatThrownBy(() -> categoryService.delete(parent.id()))
                .isInstanceOf(CategoryHasChildrenException.class);
        assertThat(categoryService.getById(parent.id())).isNotNull();
        assertThat(categoryService.getById(child.id())).isNotNull();

        categoryService.delete(child.id());
        categoryService.delete(parent.id());

        assertThat(categoryService.listFlat()).noneMatch(r -> r.id().equals(parent.id()) || r.id().equals(child.id()));
        assertThat(categoryService.listTree()).noneMatch(n -> n.id().equals(parent.id()));
    }

    @Test
    @DisplayName("soft-deleted subcategories and products do not block delete — the removal guard scopes to alive rows")
    void soft_deleted_children_and_products_do_not_block_delete() {
        String prefix = "aliveguard-" + UUID.randomUUID();
        UUID parentId = categoryService.create(
                new CategoryRequest("Parent", prefix + "-parent", null, null, null)).id();
        UUID childId = categoryService.create(
                new CategoryRequest("Child", prefix + "-child", parentId, null, null)).id();
        ProductResponse product = productService.create(new ProductRequest(
                prefix + "-SKU", "Guard", null, new BigDecimal("9.90"), null, parentId, null, null));

        productService.delete(product.id());
        categoryService.delete(childId);

        categoryService.delete(parentId);

        assertThat(categories.findById(parentId)).isEmpty();
        assertThat(categoryService.listFlat()).noneMatch(r -> r.id().equals(parentId));
    }

    @Test
    @DisplayName("delete racing a concurrent product create never leaves an alive product on a soft-deleted category")
    void delete_racing_product_create_never_leaves_orphan_product() throws Exception {
        for (int iteration = 0; iteration < 10; iteration++) {
            String prefix = "delrace-" + UUID.randomUUID();
            UUID categoryId = categoryService.create(
                    new CategoryRequest("Race", prefix, null, null, null)).id();
            String sku = prefix + "-SKU";

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);

            Future<Boolean> deleteWon = pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    categoryService.delete(categoryId);
                    return true;
                } catch (RuntimeException e) {
                    // legitimate loss: the concurrent create linked a product first
                    // (CategoryHasProductsException)
                    return false;
                }
            });
            Future<Boolean> createWon = pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    productService.create(new ProductRequest(
                            sku, "Race Product", null, new BigDecimal("9.90"), null, categoryId, null, null));
                    return true;
                } catch (RuntimeException e) {
                    // legitimate loss: the category was already soft-deleted
                    // (InvalidCategoryException / DataIntegrityViolationException)
                    return false;
                }
            });

            ready.await();
            go.countDown();
            boolean deleted = deleteWon.get();
            boolean created = createWon.get();
            pool.shutdown();

            // Both finders are alive-scoped by @SQLRestriction("deleted_at IS NULL").
            boolean categoryAlive = categories.findById(categoryId).isPresent();
            boolean productAlive = products.findBySku(sku).isPresent();

            // Invariant: an alive product must NEVER reference a soft-deleted category.
            assertThat(productAlive && !categoryAlive)
                    .as("iteration %d (deleteWon=%s, createWon=%s, categoryAlive=%s, productAlive=%s): "
                                    + "alive product referencing a soft-deleted category",
                            iteration, deleted, created, categoryAlive, productAlive)
                    .isFalse();
            // Sanity: the two outcomes are mutually exclusive losses — at least one side wins.
            assertThat(deleted || created)
                    .as("iteration %d: at least one of delete/create must succeed", iteration)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("delete racing a concurrent subcategory create never leaves an alive child on a soft-deleted category")
    void delete_racing_subcategory_create_never_leaves_orphan_child() throws Exception {
        for (int iteration = 0; iteration < 10; iteration++) {
            String prefix = "childrace-" + UUID.randomUUID();
            UUID parentId = categoryService.create(
                    new CategoryRequest("Parent", prefix, null, null, null)).id();
            String childSlug = prefix + "-child";

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);

            Future<Boolean> deleteWon = pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    categoryService.delete(parentId);
                    return true;
                } catch (RuntimeException e) {
                    // legitimate loss: the concurrent create linked a child first
                    // (CategoryHasChildrenException)
                    return false;
                }
            });
            Future<Boolean> createWon = pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    categoryService.create(new CategoryRequest("Child", childSlug, parentId, null, null));
                    return true;
                } catch (RuntimeException e) {
                    // legitimate loss: the parent was already soft-deleted
                    // (InvalidParentException)
                    return false;
                }
            });

            ready.await();
            go.countDown();
            boolean deleted = deleteWon.get();
            boolean created = createWon.get();
            pool.shutdown();

            // Both finders are alive-scoped by @SQLRestriction("deleted_at IS NULL").
            boolean parentAlive = categories.findById(parentId).isPresent();
            boolean childAlive = categories.findBySlug(childSlug).isPresent();

            // Invariant: an alive subcategory must NEVER reference a soft-deleted parent.
            assertThat(childAlive && !parentAlive)
                    .as("iteration %d (deleteWon=%s, createWon=%s, parentAlive=%s, childAlive=%s): "
                                    + "alive child referencing a soft-deleted parent",
                            iteration, deleted, created, parentAlive, childAlive)
                    .isFalse();
            assertThat(deleted || created)
                    .as("iteration %d: at least one of delete/create must succeed", iteration)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("delete racing a concurrent re-parent never leaves an alive child on a soft-deleted category")
    void delete_racing_reparent_never_leaves_orphan_child() throws Exception {
        for (int iteration = 0; iteration < 10; iteration++) {
            String prefix = "reparentrace-" + UUID.randomUUID();
            UUID parentId = categoryService.create(
                    new CategoryRequest("Parent", prefix + "-parent", null, null, null)).id();
            String childSlug = prefix + "-child";
            UUID childId = categoryService.create(
                    new CategoryRequest("Child", childSlug, null, null, null)).id();

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);

            Future<Boolean> deleteWon = pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    categoryService.delete(parentId);
                    return true;
                } catch (RuntimeException e) {
                    // legitimate loss: the concurrent re-parent linked the child first
                    // (CategoryHasChildrenException)
                    return false;
                }
            });
            Future<Boolean> reparentWon = pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    categoryService.update(childId, new CategoryRequest("Child", childSlug, parentId, null, null));
                    return true;
                } catch (RuntimeException e) {
                    // legitimate loss: the parent was already soft-deleted
                    // (InvalidParentException)
                    return false;
                }
            });

            ready.await();
            go.countDown();
            boolean deleted = deleteWon.get();
            boolean reparented = reparentWon.get();
            pool.shutdown();

            boolean parentAlive = categories.findById(parentId).isPresent();
            boolean childPointsToParent = categories.findById(childId)
                    .map(c -> parentId.equals(c.getParentId()))
                    .orElse(false);

            // Invariant: a child linked to the parent must NEVER coexist with a soft-deleted parent.
            assertThat(childPointsToParent && !parentAlive)
                    .as("iteration %d (deleteWon=%s, reparentWon=%s, parentAlive=%s, childPointsToParent=%s): "
                                    + "alive child referencing a soft-deleted parent",
                            iteration, deleted, reparented, parentAlive, childPointsToParent)
                    .isFalse();
            assertThat(deleted || reparented)
                    .as("iteration %d: at least one of delete/re-parent must succeed", iteration)
                    .isTrue();
        }
    }
}

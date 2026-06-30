package com.hubinity.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hubinity.catalog.api.dto.CategoryRequest;
import com.hubinity.catalog.api.dto.CategoryResponse;
import com.hubinity.catalog.api.error.CategoryHasChildrenException;
import com.hubinity.catalog.api.error.CategoryNotFoundException;
import com.hubinity.catalog.api.error.CircularReferenceException;
import com.hubinity.catalog.api.error.DuplicateSlugException;
import com.hubinity.catalog.api.error.InvalidParentException;
import com.hubinity.catalog.api.error.CategoryHasProductsException;
import com.hubinity.catalog.api.mapper.CategoryMapper;
import com.hubinity.catalog.domain.Category;
import com.hubinity.catalog.domain.CategoryRepository;
import com.hubinity.catalog.domain.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categories;

    @Mock
    private CategoryMapper mapper;

    @Mock
    private ProductRepository products;

    private CategoryService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new CategoryService(categories, mapper, products);
    }

    private Category entityWithDisplayOrder(int displayOrder) {
        Category c = new Category();
        c.setDisplayOrder(displayOrder);
        return c;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("top-level create with no existing siblings assigns displayOrder 0")
        void topLevel_noSiblings_assignsDisplayOrderZero() {
            CategoryRequest req = new CategoryRequest("Electronics", "electronics", null, null, null);
            when(categories.existsBySlug("electronics")).thenReturn(false);
            when(categories.findByParentIdIsNullOrderByDisplayOrderAsc()).thenReturn(List.of());
            Category mapped = entityWithDisplayOrder(0);
            when(mapper.toEntity(req)).thenReturn(mapped);
            Category saved = entityWithDisplayOrder(0);
            saved.setId(UUID.randomUUID());
            when(categories.save(any(Category.class))).thenReturn(saved);
            when(mapper.toResponse(saved)).thenReturn(
                    new CategoryResponse(saved.getId(), "Electronics", "electronics", null, 0, true, null, null));

            CategoryResponse result = service.create(req);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categories).save(captor.capture());
            assertThat(captor.getValue().getDisplayOrder()).isZero();
            assertThat(result.displayOrder()).isZero();
        }

        @Test
        @DisplayName("create under an existing parent assigns the next sibling position")
        void underExistingParent_assignsNextSiblingPosition() {
            UUID parentId = UUID.randomUUID();
            CategoryRequest req = new CategoryRequest("Phones", "phones", parentId, null, null);
            when(categories.existsById(parentId)).thenReturn(true);
            when(categories.existsBySlug("phones")).thenReturn(false);
            when(categories.findByParentIdOrderByDisplayOrderAsc(parentId))
                    .thenReturn(List.of(entityWithDisplayOrder(0), entityWithDisplayOrder(1)));
            Category mapped = entityWithDisplayOrder(0);
            when(mapper.toEntity(req)).thenReturn(mapped);
            Category saved = entityWithDisplayOrder(2);
            when(categories.save(any(Category.class))).thenReturn(saved);
            when(mapper.toResponse(saved)).thenReturn(
                    new CategoryResponse(UUID.randomUUID(), "Phones", "phones", parentId, 2, true, null, null));

            service.create(req);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categories).save(captor.capture());
            assertThat(captor.getValue().getDisplayOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("explicit displayOrder is respected as-is, not recomputed")
        void explicitDisplayOrder_isRespected() {
            CategoryRequest req = new CategoryRequest("Electronics", "electronics", null, 7, null);
            when(categories.existsBySlug("electronics")).thenReturn(false);
            Category mapped = entityWithDisplayOrder(7);
            when(mapper.toEntity(req)).thenReturn(mapped);
            when(categories.save(any(Category.class))).thenReturn(mapped);
            when(mapper.toResponse(mapped)).thenReturn(
                    new CategoryResponse(UUID.randomUUID(), "Electronics", "electronics", null, 7, true, null, null));

            service.create(req);

            verify(categories, never()).findByParentIdIsNullOrderByDisplayOrderAsc();
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categories).save(captor.capture());
            assertThat(captor.getValue().getDisplayOrder()).isEqualTo(7);
        }

        @Test
        @DisplayName("duplicate slug throws DuplicateSlugException and never saves")
        void duplicateSlug_throws() {
            CategoryRequest req = new CategoryRequest("Electronics", "electronics", null, null, null);
            when(categories.existsBySlug("electronics")).thenReturn(true);

            assertThatThrownBy(() -> service.create(req)).isInstanceOf(DuplicateSlugException.class);
            verify(categories, never()).save(any());
        }

        @Test
        @DisplayName("unknown parentId throws InvalidParentException and never saves")
        void unknownParent_throws() {
            UUID parentId = UUID.randomUUID();
            CategoryRequest req = new CategoryRequest("Phones", "phones", parentId, null, null);
            when(categories.existsById(parentId)).thenReturn(false);

            assertThatThrownBy(() -> service.create(req)).isInstanceOf(InvalidParentException.class);
            verify(categories, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("field updates persist via the mapper")
        void fieldUpdate_persists() {
            UUID id = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            existing.setSlug("electronics");
            CategoryRequest req = new CategoryRequest("Electronics Updated", "electronics", null, 9, null);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.save(existing)).thenReturn(existing);
            CategoryResponse response = new CategoryResponse(id, "Electronics Updated", "electronics", null, 9, true, null, null);
            when(mapper.toResponse(existing)).thenReturn(response);

            CategoryResponse result = service.update(id, req);

            verify(mapper).updateEntity(existing, req);
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("reparenting to a different existing category succeeds")
        void reparent_toDifferentExistingCategory_succeeds() {
            UUID id = UUID.randomUUID();
            UUID newParentId = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            existing.setSlug("phones");
            CategoryRequest req = new CategoryRequest("Phones", "phones", newParentId, null, null);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsById(newParentId)).thenReturn(true);
            when(categories.findById(newParentId)).thenReturn(Optional.of(entityWithDisplayOrder(0)));
            when(categories.save(existing)).thenReturn(existing);
            when(mapper.toResponse(existing)).thenReturn(
                    new CategoryResponse(id, "Phones", "phones", newParentId, 0, true, null, null));

            service.update(id, req);

            verify(mapper).updateEntity(existing, req);
        }

        @Test
        @DisplayName("reparenting to itself throws CircularReferenceException")
        void reparent_toSelf_throwsCircularReference() {
            UUID id = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            existing.setSlug("electronics");
            CategoryRequest req = new CategoryRequest("Electronics", "electronics", id, null, null);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsById(id)).thenReturn(true);

            assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(CircularReferenceException.class);
            verify(categories, never()).save(any());
        }

        @Test
        @DisplayName("reparenting to a descendant throws CircularReferenceException")
        void reparent_toDescendant_throwsCircularReference() {
            UUID id = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            existing.setSlug("electronics");
            Category child = entityWithDisplayOrder(0);
            child.setId(childId);
            child.setParentId(id);
            CategoryRequest req = new CategoryRequest("Electronics", "electronics", childId, null, null);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsById(childId)).thenReturn(true);
            when(categories.findById(childId)).thenReturn(Optional.of(child));

            assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(CircularReferenceException.class);
            verify(categories, never()).save(any());
        }

        @Test
        @DisplayName("slug collision with another category (excluding self) throws DuplicateSlugException")
        void slugCollisionWithAnother_throws() {
            UUID id = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            existing.setSlug("electronics");
            Category other = entityWithDisplayOrder(0);
            other.setId(otherId);
            other.setSlug("phones");
            CategoryRequest req = new CategoryRequest("Electronics", "phones", null, null, null);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.findBySlug("phones")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(DuplicateSlugException.class);
            verify(categories, never()).save(any());
        }

        @Test
        @DisplayName("keeping the same slug on update does not throw DuplicateSlugException")
        void sameSlugOnUpdate_doesNotThrow() {
            UUID id = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            existing.setSlug("electronics");
            CategoryRequest req = new CategoryRequest("Electronics Renamed", "electronics", null, null, null);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.findBySlug("electronics")).thenReturn(Optional.of(existing));
            when(categories.save(existing)).thenReturn(existing);
            when(mapper.toResponse(existing)).thenReturn(
                    new CategoryResponse(id, "Electronics Renamed", "electronics", null, 0, true, null, null));

            service.update(id, req);

            verify(categories).save(existing);
        }

        @Test
        @DisplayName("unknown id throws CategoryNotFoundException")
        void unknownId_throwsNotFound() {
            UUID id = UUID.randomUUID();
            CategoryRequest req = new CategoryRequest("Electronics", "electronics", null, null, null);
            when(categories.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(id, req)).isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listFlat / listTree")
    class ListAndTree {

        private Category category(UUID id, UUID parentId, String name, int displayOrder, boolean active) {
            Category c = new Category();
            c.setId(id);
            c.setParentId(parentId);
            c.setName(name);
            c.setSlug(name.toLowerCase());
            c.setDisplayOrder(displayOrder);
            c.setActive(active);
            return c;
        }

        @Test
        @DisplayName("listFlat returns all alive categories unpaginated")
        void listFlat_returnsAll() {
            Category a = category(UUID.randomUUID(), null, "A", 0, true);
            Category b = category(UUID.randomUUID(), null, "B", 1, true);
            when(categories.findAll()).thenReturn(List.of(a, b));
            when(mapper.toResponse(a)).thenReturn(
                    new CategoryResponse(a.getId(), "A", "a", null, 0, true, null, null));
            when(mapper.toResponse(b)).thenReturn(
                    new CategoryResponse(b.getId(), "B", "b", null, 1, true, null, null));

            assertThat(service.listFlat()).hasSize(2);
        }

        @Test
        @DisplayName("listFlat returns an empty list when no categories exist")
        void listFlat_empty() {
            when(categories.findAll()).thenReturn(List.of());

            assertThat(service.listFlat()).isEmpty();
        }

        @Test
        @DisplayName("listTree returns an empty list when no categories exist")
        void listTree_empty() {
            when(categories.findAll()).thenReturn(List.of());

            assertThat(service.listTree()).isEmpty();
        }

        @Test
        @DisplayName("listTree assembles a 5+ level chain with no artificial depth cap")
        void listTree_assemblesDeepChain() {
            UUID rootId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            UUID grandchildId = UUID.randomUUID();
            UUID greatId = UUID.randomUUID();
            UUID greatGreatId = UUID.randomUUID();
            Category root = category(rootId, null, "Root", 0, true);
            Category child = category(childId, rootId, "Child", 0, true);
            Category grandchild = category(grandchildId, childId, "Grandchild", 0, true);
            Category great = category(greatId, grandchildId, "Great", 0, true);
            Category greatGreat = category(greatGreatId, greatId, "GreatGreat", 0, true);
            when(categories.findAll()).thenReturn(List.of(root, child, grandchild, great, greatGreat));

            List<com.hubinity.catalog.api.dto.CategoryTreeNode> tree = service.listTree();

            assertThat(tree).hasSize(1);
            var node = tree.get(0);
            for (int level = 0; level < 4; level++) {
                assertThat(node.children()).hasSize(1);
                node = node.children().get(0);
            }
            assertThat(node.name()).isEqualTo("GreatGreat");
            assertThat(node.children()).isEmpty();
        }

        @Test
        @DisplayName("listTree includes an active=false category with its actual flag value, not excluded")
        void listTree_includesInactiveCategory() {
            Category inactive = category(UUID.randomUUID(), null, "Inactive", 0, false);
            when(categories.findAll()).thenReturn(List.of(inactive));

            List<com.hubinity.catalog.api.dto.CategoryTreeNode> tree = service.listTree();

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).active()).isFalse();
        }

        @Test
        @DisplayName("listFlat includes an active=false category")
        void listFlat_includesInactiveCategory() {
            Category inactive = category(UUID.randomUUID(), null, "Inactive", 0, false);
            when(categories.findAll()).thenReturn(List.of(inactive));
            when(mapper.toResponse(inactive)).thenReturn(
                    new CategoryResponse(inactive.getId(), "Inactive", "inactive", null, 0, false, null, null));

            assertThat(service.listFlat()).extracting(CategoryResponse::active).containsExactly(false);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("soft-deletes a childless category")
        void noChildren_softDeletes() {
            UUID id = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsByParentId(id)).thenReturn(false);

            service.delete(id);

            assertThat(existing.getDeletedAt()).isNotNull();
            verify(categories).save(existing);
        }

        @Test
        @DisplayName("throws CategoryHasChildrenException when a child exists, and never saves")
        void hasChildren_throwsAndNeverSaves() {
            UUID id = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsByParentId(id)).thenReturn(true);

            assertThatThrownBy(() -> service.delete(id)).isInstanceOf(CategoryHasChildrenException.class);
            verify(categories, never()).save(any());
        }

        @Test
        @DisplayName("unknown id throws CategoryNotFoundException")
        void unknownId_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(categories.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(id)).isInstanceOf(CategoryNotFoundException.class);
        }

        @Test
        @DisplayName("throws CategoryHasProductsException when a linked product exists, and never saves")
        void hasLinkedProducts_throwsAndNeverSaves() {
            UUID id = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsByParentId(id)).thenReturn(false);
            when(products.existsByCategoryId(id)).thenReturn(true);

            assertThatThrownBy(() -> service.delete(id)).isInstanceOf(CategoryHasProductsException.class);
            verify(categories, never()).save(any());
        }

        @Test
        @DisplayName("a category with neither children nor linked products still soft-deletes successfully")
        void noChildrenNoProducts_softDeletes() {
            UUID id = UUID.randomUUID();
            Category existing = entityWithDisplayOrder(0);
            existing.setId(id);
            when(categories.findById(id)).thenReturn(Optional.of(existing));
            when(categories.existsByParentId(id)).thenReturn(false);
            when(products.existsByCategoryId(id)).thenReturn(false);

            service.delete(id);

            assertThat(existing.getDeletedAt()).isNotNull();
            verify(categories).save(existing);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns the mapped response when the category exists")
        void found_returnsResponse() {
            UUID id = UUID.randomUUID();
            Category entity = entityWithDisplayOrder(0);
            entity.setId(id);
            when(categories.findById(id)).thenReturn(Optional.of(entity));
            CategoryResponse response = new CategoryResponse(id, "Electronics", "electronics", null, 0, true, null, null);
            when(mapper.toResponse(entity)).thenReturn(response);

            assertThat(service.getById(id)).isEqualTo(response);
        }

        @Test
        @DisplayName("unknown id throws CategoryNotFoundException")
        void unknown_throwsNotFound() {
            UUID id = UUID.randomUUID();
            when(categories.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id)).isInstanceOf(CategoryNotFoundException.class);
        }
    }
}

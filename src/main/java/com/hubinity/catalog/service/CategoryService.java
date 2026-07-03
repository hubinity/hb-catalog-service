package com.hubinity.catalog.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hubinity.catalog.api.dto.CategoryRequest;
import com.hubinity.catalog.api.dto.CategoryResponse;
import com.hubinity.catalog.api.dto.CategoryTreeNode;
import com.hubinity.catalog.api.error.CategoryHasChildrenException;
import com.hubinity.catalog.api.error.CategoryHasProductsException;
import com.hubinity.catalog.api.error.CategoryNotFoundException;
import com.hubinity.catalog.api.error.CircularReferenceException;
import com.hubinity.catalog.api.error.DuplicateSlugException;
import com.hubinity.catalog.api.error.InvalidParentException;
import com.hubinity.catalog.api.mapper.CategoryMapper;
import com.hubinity.catalog.domain.Category;
import com.hubinity.catalog.domain.CategoryRepository;
import com.hubinity.catalog.domain.ProductRepository;

/**
 * Business rules for {@code Category} CRUD and tree assembly that don't belong
 * in the controller (validation against existing data, parent/slug checks,
 * display-order assignment) or in the entity/repository (pure persistence).
 */
@Service
public class CategoryService {

    /** {@code Collectors.groupingBy} rejects null keys; stands in for "no parent" (root level). */
    private static final UUID ROOT = new UUID(0L, 0L);

    private final CategoryRepository categories;
    private final CategoryMapper mapper;
    private final ProductRepository products;

    public CategoryService(CategoryRepository categories, CategoryMapper mapper, ProductRepository products) {
        this.categories = categories;
        this.mapper = mapper;
        this.products = products;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (request.parentId() != null && !categories.existsById(request.parentId())) {
            throw new InvalidParentException(request.parentId());
        }
        if (categories.existsBySlug(request.slug())) {
            throw new DuplicateSlugException(request.slug());
        }

        Category entity = mapper.toEntity(request);
        if (request.displayOrder() == null) {
            entity.setDisplayOrder(nextDisplayOrder(request.parentId()));
        }

        Category saved = categories.save(entity);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        Category entity = categories.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        return mapper.toResponse(entity);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category entity = categories.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));

        if (request.parentId() != null) {
            if (!categories.existsById(request.parentId())) {
                throw new InvalidParentException(request.parentId());
            }
            assertNoCycle(id, request.parentId());
        }

        categories.findBySlug(request.slug())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateSlugException(request.slug());
                });

        mapper.updateEntity(entity, request);
        Category saved = categories.save(entity);
        return mapper.toResponse(saved);
    }

    private void assertNoCycle(UUID categoryId, UUID proposedParentId) {
        UUID current = proposedParentId;
        while (current != null) {
            if (current.equals(categoryId)) {
                throw new CircularReferenceException(categoryId, proposedParentId);
            }
            current = categories.findById(current).map(Category::getParentId).orElse(null);
        }
    }

    /**
     * Act first, diagnose after: the atomic conditional {@code UPDATE} is the
     * single authoritative guard-and-action ({@link
     * CategoryRepository#softDeleteIfRemovable}); the follow-up reads only
     * pick the right ProblemDetail when it reports 0 rows.
     */
    @Transactional
    public void delete(UUID id) {
        if (categories.softDeleteIfRemovable(id) == 1) {
            // Fresh-snapshot re-check: if the UPDATE above was blocked by a
            // concurrent create holding the category row lock (touchIfAlive),
            // that create has committed by now but was invisible to the
            // UPDATE's own statement snapshot (EvalPlanQual re-checks only the
            // target row, not the NOT EXISTS subqueries). Throwing here rolls
            // the soft delete back.
            if (categories.existsByParentId(id)) {
                throw new CategoryHasChildrenException(id);
            }
            if (products.existsByCategoryId(id)) {
                throw new CategoryHasProductsException(id);
            }
            return;
        }
        categories.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        if (categories.existsByParentId(id)) {
            throw new CategoryHasChildrenException(id);
        }
        throw new CategoryHasProductsException(id);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listFlat() {
        return categories.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeNode> listTree() {
        Map<UUID, List<Category>> byParent = categories.findAll().stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? ROOT : c.getParentId()));
        return buildTreeNodes(ROOT, byParent);
    }

    private List<CategoryTreeNode> buildTreeNodes(UUID parentId, Map<UUID, List<Category>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .map(c -> new CategoryTreeNode(
                        c.getId(), c.getName(), c.getSlug(), c.getDisplayOrder(), c.isActive(),
                        buildTreeNodes(c.getId(), byParent)))
                .toList();
    }

    private int nextDisplayOrder(UUID parentId) {
        List<Category> siblings = parentId == null
                ? categories.findByParentIdIsNullOrderByDisplayOrderAsc()
                : categories.findByParentIdOrderByDisplayOrderAsc(parentId);
        return siblings.isEmpty() ? 0 : siblings.get(siblings.size() - 1).getDisplayOrder() + 1;
    }
}

package com.hubinity.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hubinity.catalog.api.dto.PriceHistoryResponse;
import com.hubinity.catalog.api.dto.ProductPageResponse;
import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.dto.ProductResponse;
import com.hubinity.catalog.api.error.DuplicateSkuException;
import com.hubinity.catalog.api.error.InvalidCategoryException;
import com.hubinity.catalog.api.error.InvalidPaginationException;
import com.hubinity.catalog.api.error.ProductHasStockOrReservationsException;
import com.hubinity.catalog.api.error.ProductNotFoundException;
import com.hubinity.catalog.api.mapper.ProductMapper;
import com.hubinity.catalog.domain.CategoryRepository;
import com.hubinity.catalog.domain.PriceHistory;
import com.hubinity.catalog.domain.PriceHistoryRepository;
import com.hubinity.catalog.domain.Product;
import com.hubinity.catalog.domain.ProductRepository;
import com.hubinity.catalog.domain.StockItem;
import com.hubinity.catalog.domain.StockItemRepository;
import com.hubinity.catalog.domain.StockReservationRepository;
import com.hubinity.catalog.integration.EventPublisher;

/**
 * Business rules for {@code Product} CRUD, search/pagination, price-history
 * recording, and the stock/reservation removal guard.
 */
@Service
public class ProductService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "price", "sku", "createdAt");

    private final ProductRepository products;
    private final ProductMapper mapper;
    private final CategoryRepository categories;
    private final PriceHistoryRepository priceHistory;
    private final StockItemRepository stockItems;
    private final StockReservationRepository stockReservations;
    private final EventPublisher eventPublisher;

    public ProductService(
            ProductRepository products,
            ProductMapper mapper,
            CategoryRepository categories,
            PriceHistoryRepository priceHistory,
            StockItemRepository stockItems,
            StockReservationRepository stockReservations,
            EventPublisher eventPublisher) {
        this.products = products;
        this.mapper = mapper;
        this.categories = categories;
        this.priceHistory = priceHistory;
        this.stockItems = stockItems;
        this.stockReservations = stockReservations;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (!categories.existsById(request.categoryId())) {
            throw new InvalidCategoryException(request.categoryId());
        }
        if (products.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Product entity = mapper.toEntity(request);
        Product saved = products.save(entity);
        priceHistory.save(new PriceHistory(saved.getId(), saved.getPrice()));
        stockItems.save(new StockItem(saved.getId()));
        eventPublisher.publishProductCreated(saved);
        return mapper.toResponse(saved);
    }

    public ProductResponse getById(UUID id) {
        Product entity = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        Product entity = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        int updated = products.softDeleteIfRemovable(id);
        if (updated == 0) {
            throw new ProductHasStockOrReservationsException(id);
        }

        eventPublisher.publishProductDeactivated(entity);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product entity = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        if (!categories.existsById(request.categoryId())) {
            throw new InvalidCategoryException(request.categoryId());
        }

        products.findBySku(request.sku())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateSkuException(request.sku());
                });

        boolean priceChanged = entity.getPrice().compareTo(request.price()) != 0;
        BigDecimal previousPrice = entity.getPrice();
        mapper.updateEntity(entity, request);
        Product saved = products.save(entity);
        if (priceChanged) {
            priceHistory.save(new PriceHistory(saved.getId(), saved.getPrice()));
            eventPublisher.publishPriceChanged(saved, previousPrice);
        }
        eventPublisher.publishProductUpdated(saved);
        return mapper.toResponse(saved);
    }

    public List<PriceHistoryResponse> getPriceHistory(UUID productId) {
        List<PriceHistory> entries = priceHistory.findByProductIdOrderByCreatedAtDesc(productId);
        if (entries.isEmpty()) {
            throw new ProductNotFoundException(productId);
        }
        return entries.stream()
                .map(e -> new PriceHistoryResponse(e.getId(), e.getProductId(), e.getPrice(), e.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public ProductPageResponse search(UUID categoryId, String q, int page, int size, String sort) {
        if (size < 1 || size > 100) {
            throw new InvalidPaginationException("page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Specification<Product> spec = buildSpecification(categoryId, q);
        Page<Product> result = products.findAll(spec, pageable);

        List<ProductResponse> content = result.getContent().stream().map(mapper::toResponse).toList();
        return new ProductPageResponse(
                content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String field = parts[0];
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new InvalidPaginationException("sort field not supported: " + field);
        }
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private Specification<Product> buildSpecification(UUID categoryId, String q) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("categoryId"), categoryId));
        }
        if (q != null && !q.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("sku")), "%" + q.toLowerCase() + "%")));
        }
        return spec;
    }
}

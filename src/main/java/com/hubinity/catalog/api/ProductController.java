package com.hubinity.catalog.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hubinity.catalog.api.dto.PriceHistoryResponse;
import com.hubinity.catalog.api.dto.ProductPageResponse;
import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.dto.ProductResponse;
import com.hubinity.catalog.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** REST endpoints for {@code Product} CRUD, search/pagination, and price history. */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product CRUD, filter/search/pagination, and immutable price history")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Create a product", description = "Requires the admin role. Records the initial price-history entry.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Duplicate SKU")
    @ApiResponse(responseCode = "422", description = "Invalid category")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not found")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List/filter/search products, paginated",
            description = "Optional categoryId filter and q (name or SKU substring) search. "
                    + "size: 1-100 (default 20); sort: name|price|sku|createdAt (default name,asc).")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid page size or sort field")
    public ProductPageResponse search(
            @Parameter(description = "Filter to a single category") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Case-insensitive substring match against name or SKU")
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Format: field,direction — allowed fields: name, price, sku, createdAt")
            @RequestParam(defaultValue = "name,asc") String sort) {
        return productService.search(categoryId, q, page, size, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Update a product", description = "Requires the admin role. "
            + "A price change records a new price-history entry; an unchanged price does not.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ApiResponse(responseCode = "409", description = "Duplicate SKU")
    @ApiResponse(responseCode = "422", description = "Invalid category")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @GetMapping("/{id}/price-history")
    @Operation(summary = "Get a product's complete price history",
            description = "Newest first. Available even for a removed product.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not found (no history rows exist for this id)")
    public List<PriceHistoryResponse> getPriceHistory(@PathVariable UUID id) {
        return productService.getPriceHistory(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Remove a product",
            description = "Soft-delete; requires the admin role. Blocked while stock or active reservations exist.")
    @ApiResponse(responseCode = "204", description = "No content")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ApiResponse(responseCode = "409", description = "Product has stock or active reservations")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

package com.hubinity.catalog.api;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.hubinity.catalog.api.dto.StockMovementPageResponse;
import com.hubinity.catalog.api.dto.StockMovementRequest;
import com.hubinity.catalog.api.dto.StockMovementResult;
import com.hubinity.catalog.api.dto.StockReservationRequest;
import com.hubinity.catalog.api.dto.StockReservationResult;
import com.hubinity.catalog.service.StockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST endpoints for stock movements (IN/OUT) and the reservation lifecycle
 * (reserve/release/commit). All four mutating endpoints additionally require a
 * client-supplied {@code Idempotency-Key} header, enforced by {@code IdempotencyFilter}
 * before any of these methods run — not represented as a method parameter here since the
 * filter either short-circuits the request (replay/conflict/missing-header/in-progress)
 * or lets it through unchanged.
 */
@RestController
@Tag(name = "Stock", description = "Stock movements (IN/OUT) and the reserve/release/commit reservation lifecycle")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/api/v1/products/{productId}/stock/movements")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Record an inbound or outbound stock movement", description = "Requires the admin role and an Idempotency-Key header. type must be IN or OUT.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed, or type is not IN/OUT")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Insufficient stock")
    public ResponseEntity<StockMovementResult> recordMovement(
            @PathVariable UUID productId, @Valid @RequestBody StockMovementRequest request) {
        StockMovementResult result = stockService.recordMovement(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/api/v1/products/{productId}/stock/movements")
    @Operation(summary = "Get a product's complete stock movement history",
            description = "Newest first. Includes movements caused by reservations, releases, commits, and automatic expiries.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid page size")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public StockMovementPageResponse getMovementHistory(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return stockService.getMovementHistory(productId, page, size);
    }

    @PostMapping("/api/v1/stock/reservations")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Atomically reserve a quantity of a product",
            description = "Requires the admin role and an Idempotency-Key header. The availability check and the "
                    + "counter update happen as a single, race-safe database operation.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Insufficient stock")
    public ResponseEntity<StockReservationResult> reserve(@Valid @RequestBody StockReservationRequest request) {
        StockReservationResult result = stockService.reserve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/api/v1/stock/reservations/{id}/release")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Cancel an active reservation, returning its held quantity to available",
            description = "Requires the admin role and an Idempotency-Key header.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    @ApiResponse(responseCode = "409", description = "Reservation not active, or already expired")
    public StockReservationResult release(@PathVariable UUID id) {
        return stockService.release(id);
    }

    @PostMapping("/api/v1/stock/reservations/{id}/commit")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "Finalize an active reservation as a completed sale",
            description = "Requires the admin role and an Idempotency-Key header. "
                    + "Permanently deducts the held quantity; available is unchanged.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    @ApiResponse(responseCode = "409", description = "Reservation not active, or already expired")
    public StockReservationResult commit(@PathVariable UUID id) {
        return stockService.commit(id);
    }
}

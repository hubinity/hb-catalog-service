package com.hubinity.catalog.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hubinity.catalog.api.dto.StockMovementPageResponse;
import com.hubinity.catalog.api.dto.StockMovementRequest;
import com.hubinity.catalog.api.dto.StockMovementResult;
import com.hubinity.catalog.api.dto.StockReservationRequest;
import com.hubinity.catalog.api.dto.StockReservationResult;
import com.hubinity.catalog.api.error.InsufficientStockException;
import com.hubinity.catalog.api.error.InvalidPaginationException;
import com.hubinity.catalog.api.error.InvalidStockMovementTypeException;
import com.hubinity.catalog.api.error.ProductNotFoundException;
import com.hubinity.catalog.api.error.ReservationExpiredException;
import com.hubinity.catalog.api.error.ReservationNotActiveException;
import com.hubinity.catalog.api.error.ReservationNotFoundException;
import com.hubinity.catalog.api.mapper.StockItemMapper;
import com.hubinity.catalog.api.mapper.StockMovementMapper;
import com.hubinity.catalog.api.mapper.StockReservationMapper;
import com.hubinity.catalog.domain.ProductRepository;
import com.hubinity.catalog.domain.StockItem;
import com.hubinity.catalog.domain.StockItemRepository;
import com.hubinity.catalog.domain.StockMovement;
import com.hubinity.catalog.domain.StockMovementRepository;
import com.hubinity.catalog.domain.StockMovementType;
import com.hubinity.catalog.domain.StockReservation;
import com.hubinity.catalog.domain.StockReservationRepository;
import com.hubinity.catalog.domain.StockReservationStatus;
import com.hubinity.catalog.integration.EventPublisher;

/**
 * Business rules for stock movements (IN/OUT) and the reservation lifecycle
 * (reserve/release/commit), plus the periodic expiry sweep.
 *
 * <p>Every counter mutation goes through one of {@link StockItemRepository}'s
 * single-row conditional-{@code UPDATE} methods — the row-level write lock
 * Postgres takes for the duration of the statement is the sole concurrency
 * primitive (FR-005); no explicit locking. The same pattern backs
 * {@link StockReservationRepository}'s status transitions (FR-020). See
 * specs/003-stock-movement-reservation/research.md.
 */
@Service
public class StockService {

    private final StockItemRepository stockItems;
    private final StockMovementRepository stockMovements;
    private final StockReservationRepository stockReservations;
    private final ProductRepository products;
    private final StockMovementMapper movementMapper;
    private final StockReservationMapper reservationMapper;
    private final StockItemMapper stockItemMapper;
    private final EventPublisher eventPublisher;
    private final ReservationExpiryService reservationExpiryService;

    public StockService(
            StockItemRepository stockItems,
            StockMovementRepository stockMovements,
            StockReservationRepository stockReservations,
            ProductRepository products,
            StockMovementMapper movementMapper,
            StockReservationMapper reservationMapper,
            StockItemMapper stockItemMapper,
            EventPublisher eventPublisher,
            ReservationExpiryService reservationExpiryService) {
        this.stockItems = stockItems;
        this.stockMovements = stockMovements;
        this.stockReservations = stockReservations;
        this.products = products;
        this.movementMapper = movementMapper;
        this.reservationMapper = reservationMapper;
        this.stockItemMapper = stockItemMapper;
        this.eventPublisher = eventPublisher;
        this.reservationExpiryService = reservationExpiryService;
    }

    @Transactional
    public StockMovementResult recordMovement(UUID productId, StockMovementRequest request) {
        if (request.type() != StockMovementType.IN && request.type() != StockMovementType.OUT) {
            throw new InvalidStockMovementTypeException(request.type());
        }
        if (!products.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        int updated = request.type() == StockMovementType.IN
                ? stockItems.increaseAvailable(productId, request.quantity())
                : stockItems.decreaseAvailableIfSufficient(productId, request.quantity());

        if (updated == 0) {
            StockItem current = stockItems.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
            throw new InsufficientStockException(productId, request.quantity(), current.getAvailable());
        }

        StockMovement movement = movementMapper.toEntity(request, productId);
        StockMovement saved = stockMovements.save(movement);
        StockItem stockItem = stockItems.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        eventPublisher.publishStockChanged(productId, stockItem.getAvailable(), stockItem.getReserved(),
                request.type(), request.quantity(), saved.getOccurredAt());
        return new StockMovementResult(movementMapper.toResponse(saved), stockItemMapper.toResponse(stockItem));
    }

    public StockMovementPageResponse getMovementHistory(UUID productId, int page, int size) {
        if (!products.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        if (size < 1 || size > 100) {
            throw new InvalidPaginationException("page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<StockMovement> result = stockMovements.findByProductIdOrderByOccurredAtDesc(productId, pageable);
        return new StockMovementPageResponse(
                result.getContent().stream().map(movementMapper::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public StockReservationResult reserve(StockReservationRequest request) {
        if (!products.existsById(request.productId())) {
            throw new ProductNotFoundException(request.productId());
        }

        int updated = stockItems.reserveIfAvailable(request.productId(), request.quantity());
        if (updated == 0) {
            StockItem current = stockItems.findById(request.productId())
                    .orElseThrow(() -> new ProductNotFoundException(request.productId()));
            throw new InsufficientStockException(request.productId(), request.quantity(), current.getAvailable());
        }

        StockReservation reservation = reservationMapper.toEntity(request);
        StockReservation saved = stockReservations.save(reservation);

        Instant reservedAt = Instant.now();
        StockMovement movement = new StockMovement();
        movement.setProductId(request.productId());
        movement.setType(StockMovementType.RESERVE);
        movement.setQuantity(request.quantity());
        movement.setOccurredAt(reservedAt);
        stockMovements.save(movement);

        StockItem stockItem = stockItems.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        eventPublisher.publishStockChanged(request.productId(), stockItem.getAvailable(), stockItem.getReserved(),
                StockMovementType.RESERVE, request.quantity(), reservedAt);
        return new StockReservationResult(reservationMapper.toResponse(saved), stockItemMapper.toResponse(stockItem));
    }

    @Transactional
    public StockReservationResult commit(UUID reservationId) {
        StockReservation reservation = stockReservations.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        Instant now = Instant.now();
        int updated = stockReservations.transitionIfActive(reservationId, StockReservationStatus.COMMITTED, now);
        if (updated == 0) {
            failNotActiveOrExpired(reservationId, reservation, now);
        }

        stockItems.commitReserved(reservation.getProductId(), reservation.getQuantity());

        StockMovement movement = new StockMovement();
        movement.setProductId(reservation.getProductId());
        movement.setType(StockMovementType.COMMIT);
        movement.setQuantity(reservation.getQuantity());
        movement.setOccurredAt(now);
        stockMovements.save(movement);

        StockReservationResult result = buildReservationResult(reservationId, reservation.getProductId());
        StockItem stockItem = stockItems.findById(reservation.getProductId()).orElseThrow();
        eventPublisher.publishStockChanged(reservation.getProductId(), stockItem.getAvailable(), stockItem.getReserved(),
                StockMovementType.COMMIT, reservation.getQuantity(), now);
        return result;
    }

    @Transactional
    public StockReservationResult release(UUID reservationId) {
        StockReservation reservation = stockReservations.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        Instant now = Instant.now();
        int updated = stockReservations.transitionIfActive(reservationId, StockReservationStatus.RELEASED, now);
        if (updated == 0) {
            failNotActiveOrExpired(reservationId, reservation, now);
        }

        stockItems.releaseReservedToAvailable(reservation.getProductId(), reservation.getQuantity());

        StockMovement movement = new StockMovement();
        movement.setProductId(reservation.getProductId());
        movement.setType(StockMovementType.RELEASE);
        movement.setQuantity(reservation.getQuantity());
        movement.setOccurredAt(now);
        stockMovements.save(movement);

        StockReservationResult result = buildReservationResult(reservationId, reservation.getProductId());
        StockItem stockItem = stockItems.findById(reservation.getProductId()).orElseThrow();
        eventPublisher.publishStockChanged(reservation.getProductId(), stockItem.getAvailable(), stockItem.getReserved(),
                StockMovementType.RELEASE, reservation.getQuantity(), now);
        return result;
    }

    /**
     * Periodic safety net (US5, FR-011): catches reservations a caller never committed or
     * released in time. Each candidate goes through {@link ReservationExpiryService#expireOne},
     * so a reservation already handled by a concurrent caller (or a previous sweep tick) between
     * the finder query and this row's transition is simply a no-op, not an error (FR-020).
     */
    @Scheduled(fixedRate = 60_000)
    public void sweepExpiredReservations() {
        Instant now = Instant.now();
        stockReservations.findByStatusAndExpiresAtBefore(StockReservationStatus.ACTIVE, now)
                .forEach(r -> reservationExpiryService.expireOne(r.getId()));
    }

    /**
     * Called after a commit/release's {@code transitionIfActive} affected 0 rows: distinguishes
     * "already committed/released" from "active but past its expiry, never swept yet" — the
     * latter triggers the lazy-expiry side effect (FR-010) before throwing.
     */
    private void failNotActiveOrExpired(UUID reservationId, StockReservation reservationAtCallStart, Instant now) {
        StockReservation current = stockReservations.findById(reservationId).orElse(reservationAtCallStart);
        if (current.getStatus() == StockReservationStatus.ACTIVE && !current.getExpiresAt().isAfter(now)) {
            reservationExpiryService.expireOne(reservationId);
            throw new ReservationExpiredException(reservationId);
        }
        throw new ReservationNotActiveException(reservationId, current.getStatus());
    }

    private StockReservationResult buildReservationResult(UUID reservationId, UUID productId) {
        StockReservation reservation = stockReservations.findById(reservationId).orElseThrow();
        StockItem stockItem = stockItems.findById(productId).orElseThrow();
        return new StockReservationResult(reservationMapper.toResponse(reservation), stockItemMapper.toResponse(stockItem));
    }
}

package com.hubinity.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.hubinity.catalog.api.dto.StockItemResponse;
import com.hubinity.catalog.api.dto.StockMovementRequest;
import com.hubinity.catalog.api.dto.StockMovementResponse;
import com.hubinity.catalog.api.dto.StockReservationRequest;
import com.hubinity.catalog.api.dto.StockReservationResponse;
import com.hubinity.catalog.domain.error.InsufficientStockException;
import com.hubinity.catalog.api.error.InvalidPaginationException;
import com.hubinity.catalog.domain.error.InvalidStockMovementTypeException;
import com.hubinity.catalog.domain.error.ProductNotFoundException;
import com.hubinity.catalog.domain.error.ReservationExpiredException;
import com.hubinity.catalog.domain.error.ReservationNotActiveException;
import com.hubinity.catalog.domain.error.ReservationNotFoundException;
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
import com.hubinity.catalog.service.ReservationExpiryService;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService")
class StockServiceTest {

    @Mock
    private StockItemRepository stockItems;

    @Mock
    private StockMovementRepository stockMovements;

    @Mock
    private StockReservationRepository stockReservations;

    @Mock
    private ProductRepository products;

    @Mock
    private StockMovementMapper movementMapper;

    @Mock
    private StockReservationMapper reservationMapper;

    @Mock
    private StockItemMapper stockItemMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ReservationExpiryService reservationExpiryService;

    private StockService service;

    @BeforeEach
    void setUp() {
        service = new StockService(stockItems, stockMovements, stockReservations, products,
                movementMapper, reservationMapper, stockItemMapper, eventPublisher, reservationExpiryService);
    }

    private StockItem stockItem(UUID productId, int available, int reserved) {
        StockItem item = new StockItem(productId);
        item.setAvailable(available);
        item.setReserved(reserved);
        return item;
    }

    private StockReservation reservation(UUID id, UUID productId, int quantity, StockReservationStatus status, Instant expiresAt) {
        StockReservation r = new StockReservation();
        r.setId(id);
        r.setProductId(productId);
        r.setQuantity(quantity);
        r.setStatus(status);
        r.setExpiresAt(expiresAt);
        return r;
    }

    @Nested
    @DisplayName("recordMovement")
    class RecordMovement {

        @Test
        @DisplayName("IN increases available by the requested quantity")
        void in_increasesAvailable() {
            UUID productId = UUID.randomUUID();
            StockMovementRequest request = new StockMovementRequest(StockMovementType.IN, 10, "Delivery");
            when(products.existsById(productId)).thenReturn(true);
            when(stockItems.increaseAvailable(productId, 10)).thenReturn(1);
            StockMovement entity = new StockMovement();
            when(movementMapper.toEntity(request, productId)).thenReturn(entity);
            when(stockMovements.save(entity)).thenReturn(entity);
            when(stockItems.findById(productId)).thenReturn(Optional.of(stockItem(productId, 10, 0)));
            when(movementMapper.toResponse(entity)).thenReturn(
                    new StockMovementResponse(UUID.randomUUID(), productId, StockMovementType.IN, 10, "Delivery", null, null, null));
            when(stockItemMapper.toResponse(any())).thenReturn(new StockItemResponse(productId, 10, 0, 0, null));

            var result = service.recordMovement(productId, request);

            assertThat(result.stock().available()).isEqualTo(10);
            verify(stockMovements).save(entity);
        }

        @Test
        @DisplayName("OUT with sufficient available decreases it")
        void out_sufficientDecreasesAvailable() {
            UUID productId = UUID.randomUUID();
            StockMovementRequest request = new StockMovementRequest(StockMovementType.OUT, 5, null);
            when(products.existsById(productId)).thenReturn(true);
            when(stockItems.decreaseAvailableIfSufficient(productId, 5)).thenReturn(1);
            StockMovement entity = new StockMovement();
            when(movementMapper.toEntity(request, productId)).thenReturn(entity);
            when(stockMovements.save(entity)).thenReturn(entity);
            when(stockItems.findById(productId)).thenReturn(Optional.of(stockItem(productId, 5, 0)));
            when(movementMapper.toResponse(entity)).thenReturn(
                    new StockMovementResponse(UUID.randomUUID(), productId, StockMovementType.OUT, 5, null, null, null, null));
            when(stockItemMapper.toResponse(any())).thenReturn(new StockItemResponse(productId, 5, 0, 0, null));

            var result = service.recordMovement(productId, request);

            assertThat(result.stock().available()).isEqualTo(5);
        }

        @Test
        @DisplayName("OUT with insufficient available throws InsufficientStockException and leaves no record")
        void out_insufficientThrows() {
            UUID productId = UUID.randomUUID();
            StockMovementRequest request = new StockMovementRequest(StockMovementType.OUT, 1000, null);
            when(products.existsById(productId)).thenReturn(true);
            when(stockItems.decreaseAvailableIfSufficient(productId, 1000)).thenReturn(0);
            when(stockItems.findById(productId)).thenReturn(Optional.of(stockItem(productId, 5, 0)));

            assertThatThrownBy(() -> service.recordMovement(productId, request))
                    .isInstanceOf(InsufficientStockException.class);
            verify(stockMovements, never()).save(any());
        }

        @Test
        @DisplayName("type RESERVE is rejected as invalid for this endpoint")
        void reserveType_throwsInvalidType() {
            UUID productId = UUID.randomUUID();
            StockMovementRequest request = new StockMovementRequest(StockMovementType.RESERVE, 1, null);

            assertThatThrownBy(() -> service.recordMovement(productId, request))
                    .isInstanceOf(InvalidStockMovementTypeException.class);
            verify(stockItems, never()).increaseAvailable(any(), anyInt());
            verify(stockItems, never()).decreaseAvailableIfSufficient(any(), anyInt());
        }

        @Test
        @DisplayName("unknown productId throws ProductNotFoundException")
        void unknownProduct_throwsNotFound() {
            UUID productId = UUID.randomUUID();
            StockMovementRequest request = new StockMovementRequest(StockMovementType.IN, 1, null);
            when(products.existsById(productId)).thenReturn(false);

            assertThatThrownBy(() -> service.recordMovement(productId, request))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMovementHistory")
    class GetMovementHistory {

        @Test
        @DisplayName("page size of 101 throws InvalidPaginationException")
        void pageSizeTooLarge_throws() {
            UUID productId = UUID.randomUUID();
            when(products.existsById(productId)).thenReturn(true);

            assertThatThrownBy(() -> service.getMovementHistory(productId, 0, 101))
                    .isInstanceOf(InvalidPaginationException.class);
        }

        @Test
        @DisplayName("unknown productId throws ProductNotFoundException")
        void unknownProduct_throwsNotFound() {
            UUID productId = UUID.randomUUID();
            when(products.existsById(productId)).thenReturn(false);

            assertThatThrownBy(() -> service.getMovementHistory(productId, 0, 20))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("returns a page wrapping the finder's results")
        void returnsPage() {
            UUID productId = UUID.randomUUID();
            when(products.existsById(productId)).thenReturn(true);
            StockMovement movement = new StockMovement();
            Page<StockMovement> page = new PageImpl<>(List.of(movement), PageRequest.of(0, 20), 1);
            when(stockMovements.findByProductIdOrderByOccurredAtDesc(eq(productId), any(Pageable.class))).thenReturn(page);
            when(movementMapper.toResponse(movement)).thenReturn(
                    new StockMovementResponse(UUID.randomUUID(), productId, StockMovementType.IN, 1, null, null, null, null));

            var result = service.getMovementHistory(productId, 0, 20);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        @DisplayName("sufficient available creates an ACTIVE reservation and updates counters")
        void sufficient_createsActiveReservation() {
            UUID productId = UUID.randomUUID();
            StockReservationRequest request = new StockReservationRequest(productId, 10, null, Instant.now().plusSeconds(300));
            when(products.existsById(productId)).thenReturn(true);
            when(stockItems.reserveIfAvailable(productId, 10)).thenReturn(1);
            StockReservation entity = reservation(UUID.randomUUID(), productId, 10, StockReservationStatus.ACTIVE, request.expiresAt());
            when(reservationMapper.toEntity(request)).thenReturn(entity);
            when(stockReservations.save(entity)).thenReturn(entity);
            when(stockItems.findById(productId)).thenReturn(Optional.of(stockItem(productId, 0, 10)));
            when(reservationMapper.toResponse(entity)).thenReturn(
                    new StockReservationResponse(entity.getId(), productId, 10, null, StockReservationStatus.ACTIVE, request.expiresAt(), null));
            when(stockItemMapper.toResponse(any())).thenReturn(new StockItemResponse(productId, 0, 10, 0, null));

            var result = service.reserve(request);

            assertThat(result.reservation().status()).isEqualTo(StockReservationStatus.ACTIVE);
            assertThat(result.stock().reserved()).isEqualTo(10);
            verify(stockMovements).save(any());
        }

        @Test
        @DisplayName("insufficient available throws InsufficientStockException, creates no reservation")
        void insufficient_throwsAndCreatesNothing() {
            UUID productId = UUID.randomUUID();
            StockReservationRequest request = new StockReservationRequest(productId, 1000, null, Instant.now().plusSeconds(300));
            when(products.existsById(productId)).thenReturn(true);
            when(stockItems.reserveIfAvailable(productId, 1000)).thenReturn(0);
            when(stockItems.findById(productId)).thenReturn(Optional.of(stockItem(productId, 5, 0)));

            assertThatThrownBy(() -> service.reserve(request)).isInstanceOf(InsufficientStockException.class);
            verify(stockReservations, never()).save(any());
            verify(stockMovements, never()).save(any());
        }

        @Test
        @DisplayName("unknown productId throws ProductNotFoundException")
        void unknownProduct_throwsNotFound() {
            UUID productId = UUID.randomUUID();
            StockReservationRequest request = new StockReservationRequest(productId, 1, null, Instant.now().plusSeconds(300));
            when(products.existsById(productId)).thenReturn(false);

            assertThatThrownBy(() -> service.reserve(request)).isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("commit")
    class Commit {

        @Test
        @DisplayName("ACTIVE, non-expired reservation is committed: reserved decreases, available unchanged")
        void active_commits() {
            UUID productId = UUID.randomUUID();
            UUID reservationId = UUID.randomUUID();
            StockReservation entity = reservation(reservationId, productId, 7, StockReservationStatus.ACTIVE, Instant.now().plusSeconds(300));
            when(stockReservations.findById(reservationId)).thenReturn(Optional.of(entity));
            when(stockReservations.transitionIfActive(eq(reservationId), eq(StockReservationStatus.COMMITTED), any())).thenReturn(1);
            when(stockItems.findById(productId)).thenReturn(Optional.of(stockItem(productId, 0, 0)));
            when(reservationMapper.toResponse(entity)).thenReturn(
                    new StockReservationResponse(reservationId, productId, 7, null, StockReservationStatus.COMMITTED, entity.getExpiresAt(), null));
            when(stockItemMapper.toResponse(any())).thenReturn(new StockItemResponse(productId, 0, 0, 0, null));

            var result = service.commit(reservationId);

            assertThat(result.reservation().status()).isEqualTo(StockReservationStatus.COMMITTED);
            verify(stockItems).commitReserved(productId, 7);
            verify(stockMovements).save(any());
        }

        @Test
        @DisplayName("already-finalized reservation throws ReservationNotActiveException")
        void alreadyFinalized_throwsNotActive() {
            UUID productId = UUID.randomUUID();
            UUID reservationId = UUID.randomUUID();
            StockReservation entity = reservation(reservationId, productId, 7, StockReservationStatus.RELEASED, Instant.now().plusSeconds(300));
            when(stockReservations.findById(reservationId)).thenReturn(Optional.of(entity));
            when(stockReservations.transitionIfActive(eq(reservationId), eq(StockReservationStatus.COMMITTED), any())).thenReturn(0);

            assertThatThrownBy(() -> service.commit(reservationId)).isInstanceOf(ReservationNotActiveException.class);
            verify(stockItems, never()).commitReserved(any(), anyInt());
        }

        @Test
        @DisplayName("unknown id throws ReservationNotFoundException")
        void unknownId_throwsNotFound() {
            UUID reservationId = UUID.randomUUID();
            when(stockReservations.findById(reservationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.commit(reservationId)).isInstanceOf(ReservationNotFoundException.class);
        }

        @Test
        @DisplayName("ACTIVE but past expiresAt is lazily expired and throws ReservationExpiredException")
        void lazilyExpired_throwsExpiredAndReturnsStock() {
            UUID productId = UUID.randomUUID();
            UUID reservationId = UUID.randomUUID();
            Instant pastExpiry = Instant.now().minus(1, ChronoUnit.MINUTES);
            StockReservation entity = reservation(reservationId, productId, 7, StockReservationStatus.ACTIVE, pastExpiry);
            when(stockReservations.findById(reservationId)).thenReturn(Optional.of(entity));
            when(stockReservations.transitionIfActive(eq(reservationId), eq(StockReservationStatus.COMMITTED), any())).thenReturn(0);

            assertThatThrownBy(() -> service.commit(reservationId)).isInstanceOf(ReservationExpiredException.class);
            verify(reservationExpiryService).expireOne(reservationId);
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("ACTIVE, non-expired reservation is released: held quantity returns to available")
        void active_releases() {
            UUID productId = UUID.randomUUID();
            UUID reservationId = UUID.randomUUID();
            StockReservation entity = reservation(reservationId, productId, 4, StockReservationStatus.ACTIVE, Instant.now().plusSeconds(300));
            when(stockReservations.findById(reservationId)).thenReturn(Optional.of(entity));
            when(stockReservations.transitionIfActive(eq(reservationId), eq(StockReservationStatus.RELEASED), any())).thenReturn(1);
            when(stockItems.findById(productId)).thenReturn(Optional.of(stockItem(productId, 4, 0)));
            when(reservationMapper.toResponse(entity)).thenReturn(
                    new StockReservationResponse(reservationId, productId, 4, null, StockReservationStatus.RELEASED, entity.getExpiresAt(), null));
            when(stockItemMapper.toResponse(any())).thenReturn(new StockItemResponse(productId, 4, 0, 0, null));

            var result = service.release(reservationId);

            assertThat(result.reservation().status()).isEqualTo(StockReservationStatus.RELEASED);
            verify(stockItems).releaseReservedToAvailable(productId, 4);
        }

        @Test
        @DisplayName("already-finalized reservation throws ReservationNotActiveException")
        void alreadyFinalized_throwsNotActive() {
            UUID productId = UUID.randomUUID();
            UUID reservationId = UUID.randomUUID();
            StockReservation entity = reservation(reservationId, productId, 4, StockReservationStatus.COMMITTED, Instant.now().plusSeconds(300));
            when(stockReservations.findById(reservationId)).thenReturn(Optional.of(entity));
            when(stockReservations.transitionIfActive(eq(reservationId), eq(StockReservationStatus.RELEASED), any())).thenReturn(0);

            assertThatThrownBy(() -> service.release(reservationId)).isInstanceOf(ReservationNotActiveException.class);
        }

        @Test
        @DisplayName("unknown id throws ReservationNotFoundException")
        void unknownId_throwsNotFound() {
            UUID reservationId = UUID.randomUUID();
            when(stockReservations.findById(reservationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.release(reservationId)).isInstanceOf(ReservationNotFoundException.class);
        }

        @Test
        @DisplayName("lazily-expired reservation calls the shared expiry path and throws ReservationExpiredException")
        void lazilyExpired_throwsExpired() {
            UUID productId = UUID.randomUUID();
            UUID reservationId = UUID.randomUUID();
            Instant pastExpiry = Instant.now().minus(1, ChronoUnit.MINUTES);
            StockReservation entity = reservation(reservationId, productId, 4, StockReservationStatus.ACTIVE, pastExpiry);
            when(stockReservations.findById(reservationId)).thenReturn(Optional.of(entity));
            when(stockReservations.transitionIfActive(eq(reservationId), eq(StockReservationStatus.RELEASED), any())).thenReturn(0);

            assertThatThrownBy(() -> service.release(reservationId)).isInstanceOf(ReservationExpiredException.class);
            verify(reservationExpiryService).expireOne(reservationId);
        }
    }

    @Nested
    @DisplayName("sweepExpiredReservations")
    class SweepExpiredReservations {

        @Test
        @DisplayName("expires every overdue ACTIVE reservation returned by the finder")
        void expiresAllOverdueCandidates() {
            UUID productId = UUID.randomUUID();
            UUID r1 = UUID.randomUUID();
            UUID r2 = UUID.randomUUID();
            StockReservation res1 = reservation(r1, productId, 3, StockReservationStatus.ACTIVE, Instant.now().minusSeconds(10));
            StockReservation res2 = reservation(r2, productId, 5, StockReservationStatus.ACTIVE, Instant.now().minusSeconds(20));
            when(stockReservations.findByStatusAndExpiresAtBefore(eq(StockReservationStatus.ACTIVE), any()))
                    .thenReturn(List.of(res1, res2));

            service.sweepExpiredReservations();

            verify(reservationExpiryService).expireOne(r1);
            verify(reservationExpiryService).expireOne(r2);
        }

        @Test
        @DisplayName("a candidate already transitioned by a concurrent caller (expireIfDue returns 0) is a no-op")
        void alreadyTransitioned_isNoOp() {
            UUID reservationId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            StockReservation res = reservation(reservationId, productId, 3, StockReservationStatus.ACTIVE, Instant.now().minusSeconds(10));
            when(stockReservations.findByStatusAndExpiresAtBefore(eq(StockReservationStatus.ACTIVE), any()))
                    .thenReturn(List.of(res));

            service.sweepExpiredReservations();

            verify(reservationExpiryService).expireOne(reservationId);
        }
    }
}

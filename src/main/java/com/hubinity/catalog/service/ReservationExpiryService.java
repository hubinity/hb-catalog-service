package com.hubinity.catalog.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hubinity.catalog.domain.StockItem;
import com.hubinity.catalog.domain.StockItemRepository;
import com.hubinity.catalog.domain.StockMovement;
import com.hubinity.catalog.domain.StockMovementRepository;
import com.hubinity.catalog.domain.StockMovementType;
import com.hubinity.catalog.domain.StockReservation;
import com.hubinity.catalog.domain.StockReservationRepository;

/**
 * Componente Spring que encapsula a transição atômica de uma reserva para EXPIRED
 * — necessário para garantir contexto transacional quando chamado tanto do sweep
 * agendado (sem TX externa) quanto do caminho de lazy-expiry em commit/release
 * (dentro da TX do chamador, via propagação REQUIRED).
 */
@Service
public class ReservationExpiryService {

    private final StockReservationRepository stockReservations;
    private final StockItemRepository stockItems;
    private final StockMovementRepository stockMovements;

    public ReservationExpiryService(
            StockReservationRepository stockReservations,
            StockItemRepository stockItems,
            StockMovementRepository stockMovements) {
        this.stockReservations = stockReservations;
        this.stockItems = stockItems;
        this.stockMovements = stockMovements;
    }

    /**
     * Expira atomicamente a reserva se ainda estiver ACTIVE e vencida.
     * Usa propagação REQUIRED (padrão): se chamado de dentro de commit/release
     * (que já têm @Transactional), participa da TX existente mantendo atomicidade;
     * se chamado do sweep agendado (sem TX), inicia uma nova TX por reserva.
     */
    @Transactional
    public void expireOne(UUID reservationId) {
        StockReservation reservation = stockReservations.findById(reservationId).orElse(null);
        if (reservation == null) {
            return;
        }
        Instant now = Instant.now();
        int updated = stockReservations.expireIfDue(reservationId, now);
        if (updated == 0) {
            return;
        }

        stockItems.releaseReservedToAvailable(reservation.getProductId(), reservation.getQuantity());

        StockMovement movement = new StockMovement();
        movement.setProductId(reservation.getProductId());
        movement.setType(StockMovementType.RELEASE);
        movement.setQuantity(reservation.getQuantity());
        movement.setReason("Reservation expired");
        movement.setOccurredAt(now);
        stockMovements.save(movement);
    }
}

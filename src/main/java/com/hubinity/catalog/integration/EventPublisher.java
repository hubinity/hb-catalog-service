package com.hubinity.catalog.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.hubinity.catalog.domain.Product;
import com.hubinity.catalog.domain.StockMovementType;

/**
 * Contrato para publicação de eventos de domínio via Outbox Pattern.
 *
 * <p>Cada implementação deve gravar no outbox na <strong>mesma transação</strong>
 * do chamador (propagação REQUIRED). Consumers são responsáveis por deduplicar
 * por {@code messageId} — a entrega é at-least-once, não exactly-once.
 */
public interface EventPublisher {

    void publishProductCreated(Product product);

    void publishProductUpdated(Product product);

    void publishProductDeactivated(Product product);

    /**
     * @param productId   produto cujo estoque foi alterado
     * @param available   quantidade disponível <strong>após</strong> a mutação
     * @param reserved    quantidade reservada <strong>após</strong> a mutação
     * @param changeType  tipo de movimento que causou a alteração
     * @param quantity    quantidade movimentada
     * @param occurredAt  instante da ocorrência
     */
    void publishStockChanged(UUID productId, int available, int reserved,
                             StockMovementType changeType, int quantity, Instant occurredAt);

    void publishPriceChanged(Product product, BigDecimal previousPrice);
}

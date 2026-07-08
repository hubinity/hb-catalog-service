package com.hubinity.catalog.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * Lê um batch de mensagens PENDING em ordem FIFO (created_at, id) com row-level
     * locking. FOR UPDATE SKIP LOCKED garante que dois dispatchers concorrentes nunca
     * processem a mesma mensagem no mesmo ciclo — cada instância adquire um conjunto
     * disjunto de locks. Os locks são liberados quando a TX do chamador commita.
     *
     * <p>Deve ser chamado dentro de uma transação ativa (TransactionTemplate ou @Transactional).
     */
    @Query(value = """
            SELECT * FROM outbox_messages
            WHERE status = 'PENDING'
            ORDER BY created_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessage> findBatchForDispatch(@Param("batchSize") int batchSize);

    /**
     * Remove mensagens já publicadas com mais de {@code cutoff} dias para controle de
     * crescimento da tabela. Executado pelo OutboxHousekeepingJob uma vez por dia.
     */
    @Modifying
    @Query("DELETE FROM OutboxMessage m WHERE m.status = :status AND m.publishedAt < :cutoff")
    void deletePublishedBefore(@Param("status") OutboxMessageStatus status, @Param("cutoff") Instant cutoff);
}

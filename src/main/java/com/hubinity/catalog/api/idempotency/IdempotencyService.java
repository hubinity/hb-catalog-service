package com.hubinity.catalog.api.idempotency;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.hubinity.catalog.domain.IdempotencyRecord;
import com.hubinity.catalog.domain.IdempotencyRecordRepository;

/**
 * Serviço transacional que envolve as operações de idempotência. Necessário
 * porque {@link IdempotencyFilter} é um filtro servlet (fora do AOP do Spring)
 * e não pode chamar métodos {@code @Modifying} diretamente sem contexto transacional.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Tenta fazer o claim atômico da chave. Cada chamada precisa do seu próprio
     * commit imediato para que threads concorrentes vejam o claim.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claim(String key, String requestHash) {
        return repository.claim(key, requestHash);
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findById(String key) {
        return repository.findById(key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeRecord(String key, String requestHash, int status, String body) {
        repository.finalizeRecord(key, requestHash, status, body);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseClaim(String key) {
        repository.releaseClaim(key);
    }
}

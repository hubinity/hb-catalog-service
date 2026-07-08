# Outbox Pattern — hb-catalog-service

Implementação do **Transactional Outbox Pattern** para publicação confiável de eventos de domínio no RabbitMQ.

---

## Arquitetura

```
Business TX (ProductService / StockService)
  │
  ├── Salva entidade no Postgres
  └── DefaultEventPublisher.publish*()   ← MESMA TX ACID
        └── INSERT INTO outbox_messages (status='PENDING')

OutboxDispatcher [@Scheduled, fixedDelay=5s]
  ├── TransactionTemplate (TX curta por batch)
  │     ├── SELECT ... FOR UPDATE SKIP LOCKED
  │     ├── rabbitTemplate.send(catalog.events, routingKey, message)
  │     └── UPDATE status='PUBLISHED' | attempts++ | status='FAILED'
  └── Se attempts >= 5: send(catalog.events.dlx, "", message)  → DLQ

OutboxHousekeepingJob [@Scheduled, diariamente]
  └── DELETE WHERE status='PUBLISHED' AND published_at < now() - 7 days
```

Exchange topology:
- **`catalog.events`** — TopicExchange durable, recebe todos os eventos
- **`catalog.events.dlx`** — FanoutExchange durable (Dead Letter Exchange)
- **`catalog.events.dlq`** — Queue durable, ligada ao DLX

---

## Invariantes

> **Leia com atenção antes de consumir eventos do `catalog.events`.**

### 1. Entrega at-least-once, não exactly-once

O Outbox garante que cada evento publicado com sucesso no negócio **chegará ao broker pelo menos uma vez**, mas **pode chegar mais de uma vez** nos seguintes cenários:

- A TX do dispatcher commita (persiste `status='PUBLISHED'`) mas o `send()` ainda não foi confirmado pelo broker (publisher confirm chegou depois do commit).
- O processo reinicia entre o `send()` e a atualização do status para `PUBLISHED`.

**Consequência:** Consumers **OBRIGADOS** a deduplicar por `messageId` (header AMQP e campo `messageId` no payload JSON).

```java
// Exemplo de deduplicação no consumer
String messageId = message.getMessageProperties().getHeader("messageId");
if (processedMessageIds.contains(messageId)) return; // idempotência
processedMessageIds.add(messageId);
process(message);
```

### 2. Ordem por aggregate não garantida na reentrega

Em caso de retry (mensagem publicada mais de uma vez), a segunda entrega pode chegar fora de ordem em relação a eventos posteriores. Use o campo `occurredAt` para ordenação lógica.

### 3. Mensagens no DLQ requerem intervenção manual

Após 5 falhas, a mensagem vai para `catalog.events.dlq` e **não é reprocessada automaticamente**. A equipe de operações deve inspecionar, corrigir a causa raiz e republicamente manualmente se necessário.

---

## Como adicionar um novo evento

1. **Criar o record** em `src/main/java/com/hubinity/catalog/events/published/`:
   ```java
   public record MyNewEvent(
       @NotNull UUID aggregateId,
       // ... campos do evento
       @NotNull Instant occurredAt
   ) {}
   ```

2. **Adicionar entrada** em `CatalogEvent` enum:
   ```java
   MY_NEW_EVENT("catalog.my.new-event", "MyNewEvent", "MyAggregate"),
   ```

3. **Adicionar método** em `EventPublisher` interface:
   ```java
   void publishMyNewEvent(MyAggregate aggregate);
   ```

4. **Implementar** em `DefaultEventPublisher`:
   ```java
   @Override
   @Transactional(propagation = Propagation.REQUIRED)
   public void publishMyNewEvent(MyAggregate aggregate) {
       var event = new MyNewEvent(aggregate.getId(), Instant.now());
       persist(CatalogEvent.MY_NEW_EVENT, aggregate.getId().toString(), event);
   }
   ```

5. **Chamar** no service, dentro do método `@Transactional` que representa a operação de negócio:
   ```java
   @Transactional
   public MyAggregateResponse create(MyAggregateRequest request) {
       MyAggregate saved = repository.save(mapper.toEntity(request));
       eventPublisher.publishMyNewEvent(saved);  // mesma TX
       return mapper.toResponse(saved);
   }
   ```

6. **Adicionar teste unitário** para o service (mock do EventPublisher) e **teste de integração** para o fluxo completo.

---

## Como rodar os testes

```bash
# Testes unitários — sem Docker
mvn test

# Testes de integração — requer Docker daemon ativo
mvn -P integration-tests verify

# Classe específica
mvn -P integration-tests -Dtest=OutboxDispatcherIT verify
mvn -P integration-tests -Dtest=OutboxBrokerFailureIT verify
```

Os testes de integração do Outbox estão em:
- `OutboxDispatcherIT` — happy path, rollback de TX, 2 dispatchers concorrentes
- `OutboxBrokerFailureIT` — broker down → FAILED após 5 tentativas → DLQ

---

## Como inspecionar o outbox

```sql
-- Status geral
SELECT status, COUNT(*) FROM outbox_messages GROUP BY status;

-- Mensagens PENDING (aguardando dispatch)
SELECT id, message_id, event_type, routing_key, attempts, created_at
  FROM outbox_messages
 WHERE status = 'PENDING'
 ORDER BY created_at, id
 LIMIT 20;

-- Mensagens FAILED (requerem atenção)
SELECT id, message_id, event_type, routing_key, attempts, last_error, created_at
  FROM outbox_messages
 WHERE status = 'FAILED'
 ORDER BY created_at DESC;

-- Payload de uma mensagem específica
SELECT payload FROM outbox_messages WHERE message_id = '<uuid>';
```

---

## Como inspecionar o DLQ

**Via Management UI (dev local):**
1. Abrir `http://localhost:15672` (user: `hubinity`, password: `hubinity_local`)
2. Navegar em **Queues → catalog.events.dlq**
3. Clicar em **Get Messages** para inspecionar payloads

**Via CLI:**
```bash
# Consumir uma mensagem do DLQ sem remover (ack=false)
rabbitmqadmin get queue=catalog.events.dlq ackmode=ack_requeue_true
```

**Headers úteis em mensagens no DLQ:**
- `messageId` — UUID da mensagem para correlação com outbox_messages
- `eventType` — tipo do evento
- `x-original-routing-key` — routing key original
- `x-last-error` — último erro que causou a falha

---

## Configuração (application.yml)

| Propriedade | Padrão | Descrição |
|---|---|---|
| `app.outbox.dispatch.delay-ms` | `5000` | Intervalo entre batches (ms) |
| `app.outbox.dispatch.batch-size` | `50` | Mensagens por batch |
| `app.outbox.dispatch.max-attempts` | `5` | Tentativas antes de FAILED |
| `spring.rabbitmq.publisher-confirm-type` | `correlated` | Publisher confirms para observabilidade |
| `spring.rabbitmq.publisher-returns` | `true` | Return callback para mensagens sem rota |

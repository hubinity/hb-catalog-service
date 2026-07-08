# Análise Arquitetural — hb-catalog-service (2026-07)

Análise nível Principal Engineer do primeiro backend do ecossistema Hubinity (4 sistemas, 8 repositórios, frontend Angular em breve). Escopo: arquitetura, prontidão para frontend, preparação para microsserviços, riscos e plano de ação. Nenhuma alteração de código foi feita.

**Veredicto executivo:** o serviço está acima da média para um primeiro backend — outbox pattern real com `FOR UPDATE SKIP LOCKED`, idempotência durável em banco, concorrência de estoque via UPDATE condicional atômico, RFC 7807 consistente, 195 testes unitários + 36 de integração com Testcontainers. Os problemas encontrados são estruturais e de contrato, não de "código ruim": **inversão de dependência service→api**, **ausência de `@Transactional` em escritas multi-passo do CategoryService**, **contratos de evento definidos localmente em vez de no `contracts-events` compartilhado**, e um conjunto de inconsistências REST que vão atritar com o frontend.

---

## 1. ARQUITETURA E PADRONIZAÇÃO

### 1.1 Estrutura de pacotes — adequada, com uma violação de camadas séria

A estrutura (`api/`, `config/`, `domain/`, `integration/`, `service/`) é package-by-layer clássico do Spring, adequada ao tamanho atual. Pontos fortes: entidades sem lógica de negócio (repositórios em `domain/`, regras em `service/`), controllers finos que só orquestram, MapStruct isolando DTO↔entidade.

**Porém, a direção de dependência está invertida: `service` depende de `api`.**

- `StockService.java` importa `com.hubinity.catalog.api.dto.StockMovementResult`, `api.error.InsufficientStockException`, `api.mapper.StockMovementMapper` (linhas 14–28) e **retorna DTOs da camada API** (`StockMovementResult`, `StockReservationResult`).
- `ProductService` e `CategoryService` seguem o mesmo padrão: lançam exceções de `api.error.*` e retornam `api.dto.*Response`.

Em Clean Architecture, a camada de negócio não conhece a camada de entrega. Hoje `api` (adapters) é dependência de `service` (core). Consequências concretas:
- Impossível reusar `StockService` em um consumer RabbitMQ ou scheduler sem arrastar DTOs REST.
- As exceções "de domínio" (`InsufficientStockException`, `ReservationExpiredException`) moram no pacote do adaptador HTTP — o nome do pacote (`api.error`) já denuncia.
- Um futuro split do domínio (ex.: extrair `stock` para serviço próprio) exige desembaraçar tudo.

Não é urgente reverter — mas deve ser decidido **agora**, antes de os outros 7 repositórios copiarem o padrão. Recomendação: exceções para `domain/` (ou pacote `exception/` neutro); services retornam entidades ou modelos de domínio; mapeamento DTO↔domínio acontece no controller. Um teste ArchUnit (`noClasses().that().resideInAPackage("..service..").should().dependOnClassesThat().resideInAPackage("..api..")`) congela a regra.

### 1.2 Responsabilidade única — majoritariamente ok, com exceções pontuais

- **`StockService` acumula negócio + scheduling**: `sweepExpiredReservations()` é `@Scheduled` dentro do service de negócio. A extração recente de `ReservationExpiryService` já melhorou; o passo natural é mover também o gatilho `@Scheduled` para um componente de jobs (como já é feito com `OutboxHousekeepingJob`).
- **`DefaultEventPublisher` reconstrói estado pré-mutação por aritmética** (switch sobre `StockMovementType`, linhas 129–145) para popular `previousAvailable/previousReserved` do `StockChangedEvent`. Funciona, mas duplica invariantes do `StockItemRepository` em outro arquivo: se amanhã um movimento novo for adicionado (ex.: AJUSTE), há dois lugares para atualizar e o compilador não avisa o segundo.
- **`ProductService.getPriceHistory` usa "histórico vazio" como proxy de "produto não existe"** (linhas 131–139) — um produto legítimo sem histórico (impossível hoje porque `create` grava o primeiro registro, mas frágil a refactor) retornaria 404 incorreto. O check deveria ser `products.existsById`.

### 1.3 Acoplamento — baixo onde importa

- **FKs como `UUID` cru, sem `@ManyToOne`/`@OneToMany`** (Product.categoryId, StockMovement.productId, Category.parentId): decisão deliberada e correta para o roadmap de microsserviços — zero lazy proxies, zero N+1, agregados independentes. O banco mantém integridade real (`REFERENCES ... ON DELETE RESTRICT` em V1). Excelente trade-off.
- **`RabbitConfig` com `@ConditionalOnProperty(spring.rabbitmq.host)`**: serviço sobe sem broker — bom para testes e resiliência de boot.
- **Outbox como única ponte REST→evento**: publishers nunca falam com o broker na transação de negócio. Correto.

### 1.4 Inconsistências internas de padronização

| Inconsistência | Evidência |
|---|---|
| `StockController` sem `@RequestMapping` de classe; Category/Product têm | `StockController.java` L36–38 declara paths absolutos por método |
| POSTs de estoque (201) **não** retornam header `Location`; Category/Product retornam | `StockController.reserve` L81–83 usa `ResponseEntity.status(CREATED)` sem URI |
| `CategoryController.list` retorna `List<?>` cru | L71 — mistura `CategoryResponse` e `CategoryTreeNode` no mesmo endpoint, contrato OpenAPI ambíguo |
| `CategoryService` **não tem nenhum `@Transactional`**; Product/Stock têm nas escritas | ver risco A1 na seção 4 |
| `package-info.java` da `api` diz "Populated in features 1.5–1.7" (obsoleto) | camada já populada |
| `README-outbox.md` solto na raiz, fora de `docs/` e não commitado | working tree |

### 1.5 Dependências mortas no pom.xml

- `spring-boot-starter-cache` + `caffeine` declarados, **mas não há `@EnableCaching` nem um único `@Cacheable`** no código.
- `resilience4j-spring-boot4` + `resilience4j-reactor` declarados e há config `resilience4j.circuitbreaker` no `application.yml`, **mas nenhum `@CircuitBreaker`/`@Retry` é usado**. `resilience4j-reactor` é ainda mais estranho num stack servlet (WebMVC).
- `spring.rabbitmq.listener.simple.retry` configurado no yml, mas o serviço é **produtor-only** (não há `@RabbitListener`) — config morta que confunde o leitor.

Dependência morta não é neutra: infla o classpath, abre superfície de CVE e sinaliza intenção falsa ("esse serviço usa cache") para quem chega no código.

---

## 2. PRONTIDÃO PARA O FRONTEND

### 2.1 Pontos fortes (acima da média)

- **RFC 7807 `ProblemDetail` em 100% dos erros mapeados**, com `type` URI programático (`urn:hubinity:catalog:<slug>`) — o frontend Angular pode fazer branching por `type` em vez de parsear mensagens. 18 handlers cobrindo todo o domínio (`ApiExceptionHandler`, L34–215).
- **`handleDataIntegrityViolation` não vaza detalhes**: constraints conhecidas → 409 tipado; desconhecidas → 500 genérico com log server-side (L192–215). Segurança de exposição correta.
- **Validação Bean Validation nos 4 requests** (`@NotBlank`, `@Size`, `@Pattern` no slug, `@Positive`, `@Future` no `expiresAt`) com erros de campo agregados num map `errors` no 400.
- **Envelopes de paginação explícitos** (`ProductPageResponse`, `StockMovementPageResponse`) em vez de serializar `Page<T>` do Spring — contrato estável, imune a mudanças internas do Spring Data.
- **DTOs 100% records, zero vazamento de entidade** — nenhuma entidade JPA é serializada.
- Swagger UI habilitado em local/staging, desabilitado em prod.

### 2.2 Problemas que vão atritar com o frontend

1. **PUT com semântica de PATCH.** `ProductMapper.updateEntity` e `CategoryMapper.updateEntity` usam `NullValuePropertyMappingStrategy.IGNORE` — campos null no corpo **não sobrescrevem**. Consequência concreta: o frontend **não consegue limpar** `description`, `barcode` ou `costPrice` de um produto via PUT (enviar `"barcode": null` é ignorado silenciosamente). Ou o endpoint vira PATCH honesto, ou PUT passa a substituir integralmente — do jeito atual o formulário de edição do `hb-catalog-web` vai "salvar" e o campo volta.
2. **`List<?>` em `GET /categories`** — o codegen de client TypeScript a partir do OpenAPI vai produzir `any[]`. Separar em `GET /categories` (flat) e `GET /categories/tree`, ou dois DTOs com discriminador.
3. **`page` negativo → 500.** `search`/`getMovementHistory` validam `size` (1–100 → `InvalidPaginationException` 400), mas `page=-1` cai direto no `PageRequest.of` → `IllegalArgumentException` → 500 sem ProblemDetail tipado.
4. **Sem handler catch-all** (`@ExceptionHandler(Exception.class)`): exceções não previstas retornam o formato default do Boot, quebrando a promessa "todo erro é ProblemDetail com urn".
5. **Replay de idempotência devolve `application/json` fixo** (`IdempotencyFilter.replay`, L163–167). Respostas de erro 4xx armazenadas (ex.: 409 insufficient-stock) foram emitidas como `application/problem+json`, mas o replay as devolve com content-type errado — um interceptor Angular que filtra por content-type vai tratar o erro de forma diferente na 1ª chamada vs. no retry.
6. **`Location` ausente nos POSTs de estoque** (item 1.4) — inconsistência para clients que seguem o header.

### 2.3 Nota de contrato para o time de frontend

O header `Idempotency-Key` é **obrigatório** nos 4 POSTs de estoque (400 `idempotency-key-missing` sem ele). Isso precisa estar visível na spec OpenAPI (hoje só está em Javadoc) — senão o primeiro dev do `hb-catalog-web` perde uma hora descobrindo.

---

## 3. PREPARAÇÃO PARA MICROSSERVIÇOS

### 3.1 O que já está certo (e é raro estar)

- **Database-per-service** de fato; nenhuma tabela compartilhada.
- **Transactional Outbox completo**: gravação na TX de negócio (`DefaultEventPublisher` com `REQUIRED`), dispatcher com `FOR UPDATE SKIP LOCKED` (escala horizontal segura — réplicas pegam batches disjuntos), retry com `maxAttempts` → DLQ fanout com headers `x-original-routing-key`/`x-last-error`, housekeeping diário, índice parcial `WHERE status='PENDING'` casado com a query. Nível de produção.
- **`traceparent` W3C propagado nos eventos** (`OutboxDispatcher` L146–152) — tracing distribuído funcionará entre serviços desde o dia 1.
- **Idempotência durável em banco** (não cache), sobrevivendo a restart — pré-requisito para retry entre serviços.
- **`messageId` UUID + dedupe documentado** no contrato at-least-once do `EventPublisher`.
- **IAM centralizado** (Keycloak) com converter de roles reutilizável.

### 3.2 O maior risco distribuído: contratos de evento locais

O `pom.xml` depende de `com.hubinity:contracts-events:0.1.0-SNAPSHOT` (o módulo compartilhado existe exatamente para isso), **mas os 5 eventos publicados são records locais** em `com.hubinity.catalog.events.published.*`. Quando `sc-order-service` (Fase 3) consumir `StockChangedEvent`, terá duas opções ruins: duplicar o record na mão (drift silencioso garantido) ou importar o jar do catálogo (acoplamento produtor→consumidor). O schema do payload jsonb hoje não tem dono neutro. **Os eventos devem migrar para `platform-shared-contracts/contracts-events` (JSON Schema → jsonschema2pojo, pipeline que já existe) antes do primeiro consumidor nascer.** Depois que 2+ serviços consomem o formato ad-hoc, a migração custa 10x.

### 3.3 Acoplamentos que dificultarão o split

- **service→api** (seção 1.1): extrair o subdomínio de estoque para um `hb-stock-service` hoje arrastaria DTOs REST, mappers e exception classes do pacote `api`.
- **Idempotência e Outbox como código embutido**: os próximos 3 backends vão precisar de exatamente o mesmo `IdempotencyFilter`/`IdempotencyService`/`OutboxDispatcher`. Se cada repo copiar-colar, são 4 implementações divergindo. Candidato claro a `platform-shared-*` como starter interno (`hubinity-spring-boot-starter-outbox`) — decisão a registrar em ADR antes do segundo backend começar.

### 3.4 Lógica no banco — na medida certa

`uuidv7()` e `set_updated_at()` em plpgsql são infra, não negócio — aceitável e documentado (ADR 0009). Os UPDATEs condicionais (`WHERE available >= :qty`) não são "lógica no banco": são a primitiva de concorrência correta, com a regra visível no repositório Java. Nenhuma procedure de negócio, nenhuma trigger de negócio. Saudável.

### 3.5 Sem versionamento de evento além do rótulo

`SCHEMA_VERSION = "1.0.0"` é gravado no outbox, mas não há política de evolução (aditivo-only? consumer tolerante?). Registrar em ADR junto com a migração para `contracts-events`.

---

## 4. RISCOS TÉCNICOS

### 🔴 Alto

| # | Risco | Evidência | Impacto |
|---|---|---|---|
| A1 | **`CategoryService` sem `@Transactional` em escritas multi-passo.** `create` (valida parent → valida slug → calcula displayOrder → save), `update` (valida → checa ciclo → save), `delete` (checa filhos → checa produtos → softDelete + save) executam cada query em auto-commit isolado. | `CategoryService.java` L46–109 — zero anotações `@Transactional` | Check-then-act sem atomicidade: duas requisições concorrentes podem criar slug duplicado entre o `existsBySlug` e o `save` (mitigado pelo índice único + handler 409, ok), mas **`delete` pode soft-deletar uma categoria no exato momento em que um produto é criado nela** — produto órfão apontando para categoria deletada. Sem TX, também não há rollback conjunto se o save falhar após efeitos parciais futuros. |
| A2 | **Contratos de evento locais em vez de `contracts-events`** (seção 3.2). | `events/published/*.java` vs. dependência `contracts-events` no pom | Drift de schema entre produtor e futuros consumidores; refactor 10x mais caro após a Fase 3. |

### 🟡 Médio

| # | Risco | Evidência | Impacto |
|---|---|---|---|
| M1 | **Inversão de camadas service→api** (seção 1.1). | imports em `StockService`/`ProductService`/`CategoryService` | Manutenção e extração de serviços futuros; o padrão será clonado nos próximos 7 repos se não for corrigido agora. |
| M2 | **Retorno dos UPDATEs condicionais ignorado em `commit`/`release`/`expireOne`.** `stockItems.commitReserved(...)` e `releaseReservedToAvailable(...)` retornam contagem de linhas, que é descartada. | `StockService.java` L165, L192; `ReservationExpiryService.expireOne` | Se a invariante `reserved >= qty` for violada por qualquer bug futuro, a reserva transita para COMMITTED **sem debitar contadores** — inconsistência silenciosa, sem log, sem erro. Custo do guard: 3 linhas de `if (updated == 0) throw new IllegalStateException(...)`. |
| M3 | **PUT com semântica PATCH** (seção 2.2.1). | `NullValuePropertyMappingStrategy.IGNORE` nos mappers | Frontend não consegue limpar campos; violação da semântica HTTP; bug de UX garantido no form de edição. |
| M4 | **Sem optimistic locking em Product/Category** (nenhum `@Version` no projeto). | entidades `domain/*.java` | Dois admins editando o mesmo produto: last-write-wins silencioso. Para estoque não é problema (UPDATEs condicionais), mas para dados cadastrais é perda de escrita invisível. |
| M5 | **`page` negativo → 500** e **sem handler catch-all**. | seção 2.2.3–4 | Erros fora do contrato ProblemDetail. |
| M6 | **Replay idempotente com content-type errado** para respostas problem+json armazenadas. | `IdempotencyFilter.replay` L163–167 | Comportamento diferente na 1ª chamada vs. retry para o mesmo request. |
| M7 | **Dependências/configs mortas** (cache/caffeine, resilience4j, rabbitmq listener retry). | seção 1.5 | Superfície de CVE, sinalização falsa, ruído cognitivo. |

### 🟢 Baixo

| # | Risco | Evidência |
|---|---|---|
| B1 | `Idempotency-Key` não é escopado por principal — a chave é global. Dois clientes com a mesma key + mesmo hash compartilham replay (mesma resposta de qualquer forma); com hash diferente, um recebe 409 causado pelo outro. | `idempotency_key` PK = `key` apenas (V3) |
| B2 | `DiagnosticsController` (throwaway declarado) ainda presente; `/api/v1/_diagnostics/public` é `permitAll` em prod. Não vaza nada sensível, mas é superfície desnecessária. | `api/_diagnostics/` |
| B3 | `CategoryController.list` `List<?>`; `Location` ausente nos POSTs de estoque; `package-info` obsoleto; `README-outbox.md` fora de `docs/`. | seções 1.4/2.2 |
| B4 | Janela de corrida no re-claim de chave idempotente stale (>24h): dois retries simultâneos de chave expirada podem ambos executar o negócio. | `IdempotencyFilter` L139–142 (`finalizeRecord(key, hash, 0, "")` não é condicional) |
| B5 | `getPriceHistory` usa histórico vazio como proxy de 404. | seção 1.2 |

**Segurança geral:** sólida — default-deny (`anyRequest().denyAll()`), STATELESS, mutações gated por `hasRole('admin')` server-side, actuator sensível atrás de role, zero secret em YAML, CORS allowlist por env, erros não vazam internals. Nenhum risco Alto de segurança encontrado.

---

## 5. PLANO DE AÇÃO (top 5, por prioridade)

### 1. Fronteiras transacionais no `CategoryService` — 🔴 A1
- **Problema:** escritas multi-passo (create/update/delete) sem `@Transactional`; check-then-act não atômico.
- **Impacto:** inconsistências sob concorrência (categoria deletada com produto recém-criado); comportamento divergente do resto do codebase.
- **Recomendação:** `@Transactional` nos 3 métodos de escrita e `@Transactional(readOnly = true)` nos reads; para o par delete-categoria × cria-produto, avaliar o mesmo padrão de UPDATE condicional já usado em `ProductRepository.softDeleteIfRemovable`. Esforço: pequeno (1 sessão + testes de concorrência).

### 2. Migrar contratos de evento para `platform-shared-contracts/contracts-events` — 🔴 A2
- **Problema:** os 5 eventos publicados são records locais; o módulo compartilhado existe e já é dependência, mas não é usado.
- **Impacto:** drift de schema com os consumidores das Fases 3+; o custo de migração cresce com cada consumidor novo.
- **Recomendação:** definir JSON Schema dos 5 eventos em `contracts-events`, gerar os POJOs pelo pipeline existente (jsonschema2pojo), substituir os records locais, e registrar ADR de versionamento de eventos (aditivo-only, `schemaVersion` no envelope). **Fazer antes do primeiro consumidor** (`sc-order-service`).

### 3. Corrigir a direção de dependência service→api — 🟡 M1
- **Problema:** services importam DTOs, mappers e exceptions do pacote `api`.
- **Impacto:** domínio inextraível, padrão prestes a ser clonado em 7 repositórios.
- **Recomendação:** mover as 16 exceptions para pacote neutro (`domain` ou `exception`); services retornam entidades/modelos de domínio; controllers fazem o mapeamento. Congelar com teste ArchUnit. É o item mais caro dos cinco — pode ser incremental (exceptions primeiro, retornos depois) — mas a **decisão** precisa virar ADR agora, antes do segundo backend.

### 4. Guardar os retornos dos UPDATEs condicionais em commit/release/expire — 🟡 M2
- **Problema:** `commitReserved`/`releaseReservedToAvailable` retornam 0/1 e o valor é descartado.
- **Impacto:** violação de invariante futura viraria corrupção silenciosa de contadores de estoque — o pior tipo de bug para diagnosticar em produção.
- **Recomendação:** `if (updated == 0) throw new IllegalStateException("stock counter invariant violated: ...")` nos 3 pontos. Esforço: trivial; valor: transforma corrupção silenciosa em falha ruidosa com rollback.

### 5. Endurecer o contrato REST antes do `hb-catalog-web` — 🟡 M3/M5/M6/B3
- **Problema:** PUT-como-PATCH, `List<?>`, `page` negativo → 500, sem catch-all handler, content-type errado no replay, `Location` ausente nos POSTs de estoque, `Idempotency-Key` invisível no OpenAPI.
- **Impacto:** cada um desses vira um bug report do time de frontend nas primeiras semanas; corrigir depois quebra client code.
- **Recomendação:** um pacote único de "API hardening" (1–2 sessões): decidir PUT full-replace vs. PATCH explícito, separar `/categories/tree`, validar `page >= 0`, adicionar `@ExceptionHandler(Exception.class)` com ProblemDetail genérico, armazenar/replayar content-type na idempotência, adicionar `Location`, documentar o header no springdoc. Congela o contrato **antes** do codegen do frontend.

---

### Menções honrosas (fora do top 5)
`@Version` em Product/Category (M4) na próxima feature de CRUD; remoção das dependências mortas (M7) num commit de higiene; extração do `@Scheduled` de `StockService` para pacote de jobs; remoção do `DiagnosticsController` quando o frontend validar o fluxo de auth real; ADR para o futuro starter interno de outbox/idempotência (antes do 2º backend).

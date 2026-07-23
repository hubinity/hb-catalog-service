# Feature Specification: Especificação fina do parâmetro productId da operação de leitura de saldo

**Feature Branch**: `003-stock-productid-param`

**Created**: 2026-07-23

**Status**: Draft

**Input**: User description: "detalhe a task T-001-3 descrita no TASKS.json: 'Definir parâmetro productId da operação de leitura de saldo de estoque em contracts-catalog/openapi/catalog.yaml com base no platform-shared-contracts'. Use o código real de platform-shared-contracts como contexto técnico absoluto."

**Task de origem**: `T-001-3` (TASKS.json, fase `contracts`) — depende de `T-001-2` (**concluída**, commit `e32df53`).

## Contexto técnico verificado (código real)

Estado atual da operação `getStockItemByProductId` em `catalog.yaml` (pós T-001-2, **com contingência FR-006 acionada**):

```yaml
parameters:
  - name: productId
    in: path
    required: true
    schema:
      type: string
      format: uuid
```

- O **bloco mínimo do parâmetro já existe**: a contingência FR-006 da feature 002 foi acionada (o validador falhou com `Declared path parameter productId needs to be defined as a path parameter in path or operation level`) e o bloco acima foi entregue no mesmo commit, com registro no adendo R2 de `specs/002-stock-balance-get/research.md` — que atribui a esta task (T-001-3) a "especificação fina": description e refinamentos.
- Convenção real do parâmetro em `getProductById`: `name`/`in`/`required`/**`description: Product UUID`**/`schema (string, uuid)`. A **única diferença** entre o parâmetro entregue e a convenção é a ausência de `description`.
- A `description` do Path Item ainda afirma: "its productId parameter, response body, and authorization are declared by follow-up contract tasks (T-001-3..T-001-5)" — parcialmente desatualizada (o parâmetro já existe estruturalmente; após esta task, estará completo).

## Decisão de escopo desta task

Escopo **residual e declaratório**: (a) completar o parâmetro com a `description` da convenção; (b) **ratificar formalmente** o bloco mínimo entregue pela contingência como a definição oficial de T-001-3 (verificando aderência à convenção); (c) atualizar a `description` do Path Item para o novo estado. Ficam fora: schema de resposta (T-001-4), autorização (T-001-5) e implementação (T-004-x).

**Definição do parâmetro (estado final)**:

```yaml
- name: productId
  in: path
  required: true
  description: Product UUID
  schema:
    type: string
    format: uuid
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor entende o parâmetro sem ler outras operações (Priority: P1)

Uma equipe consumidora que inspeciona a operação de leitura de saldo encontra o parâmetro `productId` completamente documentado — incluindo a descrição do que ele é (UUID do produto) — sem precisar deduzi-lo por comparação com `getProductById` ou pelo nome do path.

**Why this priority**: É o entregável residual da task; a description é o único campo que falta para o parâmetro atingir paridade com a convenção do contrato.

**Independent Test**: Abrir `catalog.yaml` e verificar que o parâmetro da operação `getStockItemByProductId` tem os 5 campos da convenção (`name`, `in`, `required`, `description`, `schema`), com `description: Product UUID`; build do módulo permanece verde.

**Acceptance Scenarios**:

1. **Given** a operação `getStockItemByProductId`, **When** um consumidor inspeciona `parameters`, **Then** encontra exatamente 1 parâmetro com os 5 campos da convenção, incluindo `description: Product UUID`.
2. **Given** o parâmetro completado, **When** comparado ao parâmetro de `getProductById`, **Then** a estrutura é idêntica campo a campo (diferindo apenas em `name`: `productId` vs `id`).
3. **Given** a edição concluída, **When** o build do módulo de contratos roda, **Then** conclui sem erros.

---

### User Story 2 - Registro formal encerra a pendência da contingência (Priority: P2)

Quem acompanha a cadeia T-001-x (tracker, PRs) vê a pendência aberta pelo adendo R2 da feature 002 formalmente encerrada: o bloco mínimo ratificado como definição oficial, a description entregue, e o Path Item descrevendo o estado real (restam corpo de resposta e autorização).

**Why this priority**: Sem o encerramento formal, a contingência ficaria como dívida implícita — o tracker diria "parâmetro definido" sem que nenhuma entrega o tivesse ratificado.

**Independent Test**: A `description` do Path Item menciona apenas T-001-4/T-001-5 como pendências; o diff da entrega mostra somente a linha da description do parâmetro e o bloco de description do Path Item.

**Acceptance Scenarios**:

1. **Given** a `description` do Path Item, **When** lida após a entrega, **Then** declara que a operação GET e seu parâmetro estão definidos e que corpo de resposta e autorização vêm de T-001-4/T-001-5.
2. **Given** o diff da entrega, **When** inspecionado, **Then** contém apenas: +1 linha (`description: Product UUID` no parâmetro) e a reescrita do bloco de `description` do Path Item — nada mais.

---

### Edge Cases

- **Divergência entre bloco entregue e convenção**: se a verificação de ratificação encontrar qualquer diferença estrutural entre o bloco da contingência e a convenção (além da description ausente), a correção pertence a esta task — hoje, nenhuma divergência é conhecida (verificado acima).
- **Description do parâmetro vs. semântica do 404**: `Product UUID` descreve o formato/identidade; a semântica de produto inexistente (404 único) já está registrada na resposta (T-001-2, R4) e não deve ser duplicada na description do parâmetro.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O parâmetro `productId` da operação `getStockItemByProductId` MUST conter `description: Product UUID`, atingindo paridade campo a campo com o parâmetro de `getProductById` (name/in/required/description/schema string+uuid).
- **FR-002**: O bloco mínimo entregue pela contingência FR-006 da feature 002 MUST ser ratificado como definição oficial do parâmetro — a verificação de aderência à convenção faz parte desta entrega, com **evidência registrada no corpo do commit** em `platform-shared-contracts` (padrão da feature 002); divergências estruturais (se surgirem) são corrigidas aqui.
- **FR-003**: A `description` do Path Item MUST ser atualizada para o texto: "Canonical read address for a product's on-hand stock snapshot (see components/schemas/StockItem). The GET operation and its productId parameter are declared; the response body and authorization are declared by follow-up contract tasks (T-001-4, T-001-5)."
- **FR-004**: A mudança MUST ser aditiva/restrita: apenas a linha de description do parâmetro e o bloco de description do Path Item; demais paths, operações, schemas e a seção `tags` permanecem intactos; o documento MUST permanecer válido segundo a autoridade de validação (build do módulo — herdada da feature 001, R3).
- **FR-005**: Nenhum refinamento adicional de validação (pattern, exemplos, `allowEmptyValue` etc.) MUST ser adicionado — a convenção do contrato (getProductById) não os usa, e introduzi-los criaria assimetria; qualquer refinamento futuro exigiria task de contrato própria.

### Key Entities

- **Parâmetro `productId` (existente — completado)**: único parâmetro da operação; ganha `description: Product UUID`; demais campos ratificados como estão.
- **Path Item do saldo (existente — description atualizada)**: passa a refletir o estado "operação + parâmetro definidos; pendem T-001-4/5".
- **Parâmetro de `getProductById` (existente — intocado)**: referência de convenção para a paridade campo a campo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O parâmetro da operação de saldo tem os 5 campos da convenção, verificável por inspeção direta; paridade campo a campo com `getProductById` (exceto `name`).
- **SC-002**: O documento permanece válido e processável: build do módulo de contratos verde após a edição.
- **SC-003**: A pendência do adendo R2 (feature 002) está formalmente encerrada: nenhuma seção do **contrato** (`catalog.yaml`) nem dos **artefatos desta feature** afirma que o parâmetro está pendente. Os artefatos históricos da feature 002 (adendo R2) permanecem intactos — são registro da contingência, não pendência ativa.
- **SC-004**: A task T-001-4 (schema de resposta) fica desbloqueada imediatamente após a conclusão desta.

## Assumptions

- O texto `Product UUID` é a description correta por paridade com `getProductById`; nenhuma informação adicional (ex.: semântica de 404) deve ser duplicada nela.
- O bloco da contingência está estruturalmente idêntico à convenção (verificado contra o código real em 2026-07-23); FR-002 existe como salvaguarda formal, não como correção esperada.
- Autoridade de validação e workflow herdados da cadeia (feature 001 R3/R6): build do módulo como gate; branch `feature/stock-balance-path` ativa nos dois repos; commits na fase de polish.
- O vocabulário divergente `StockItemResponse` (TASKS.json) × `StockItem` (contrato) segue registrado para resolução no specify de T-001-4 (achado L2 da análise da feature 002) — fora do escopo aqui.

## Out of Scope

- Schema de resposta no `content` da `'200'` (T-001-4).
- Requisito de autorização/`security` (T-001-5).
- Refinamentos de validação do parâmetro além da convenção (FR-005).
- Implementação do endpoint no serviço (T-004-x).

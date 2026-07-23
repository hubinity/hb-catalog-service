# Research: Path canônico do endpoint de leitura de saldo de estoque (T-001-1)

**Date**: 2026-07-22 · **Plan**: [plan.md](./plan.md)

Todas as incertezas do Technical Context e os itens de maior risco do checklist (`checklists/contract.md`) foram investigados contra o código real. Nenhum NEEDS CLARIFICATION permanece.

## R1. Escolha do path

- **Decision**: `/api/v1/products/{productId}/stock`
- **Rationale**: (a) `StockItem` é sub-recurso singular 1:1 do produto (entidade `StockItem` do serviço tem PK = `product_id`); (b) o serviço já expõe `.../products/{productId}/stock/movements` — o saldo no path pai é a leitura do agregado cujo histórico são os movements; (c) `productId` é o nome usado nos endpoints de estoque e no campo do schema `StockItem` já existente no contrato.
- **Alternatives considered**:
  - `/api/v1/stock/items/{productId}` — rejeitado: no serviço, o escopo global `/api/v1/stock/*` é usado só para reservas, identificadas pelo id da reserva.
  - `/api/v1/products/{id}/stock` (reusando `id`) — rejeitado: quebraria a consistência com a hierarquia de estoque existente e com o schema `StockItem.productId`.

## R2. Path Item sem operação (contingência FR-006)

- **Decision**: Entregar a entrada de path **standalone**, sem operação GET, contendo apenas `summary`/`description` documentando a canonicidade. A contingência de FR-006 (entrega conjunta com T-001-2) fica armada mas **provavelmente não dispara**.
- **Rationale**: Na especificação OpenAPI 3.1, o Path Item Object pode ser vazio (todas as suas propriedades são opcionais). O pipeline real do módulo usa `openapi-generator-maven-plugin` com `generateApis=false` / `generateModels=true` (ADR 0002 — DTO-only): paths **não geram código algum**, apenas passam pelo parse do swagger-parser, que aceita path items sem operações. A prova definitiva é empírica: `mvn -B install` (ver R3). Se — contra a expectativa — o parse falhar, aplica-se FR-006: mesmo commit com a operação GET mínima de T-001-2.
- **Alternatives considered**: Entregar já com a operação GET completa — rejeitado: violaria o escopo acordado (somente T-001-1) e anteciparia decisões de T-001-2/3/4/5 sem specs próprios.

## R3. Autoridade de validação (resolve CHK006/CHK007)

- **Decision**: A validação oficial de "documento OpenAPI 3.1 válido" (FR-004, SC-002) é `( cd platform-shared-contracts && mvn -B -DskipTests install )` — o parse do swagger-parser embutido no `openapi-generator-maven-plugin` durante `generate-sources` do módulo `contracts-catalog`.
- **Rationale**: É o único consumidor real do arquivo no ecossistema hoje; se esse build passa, o contrato é utilizável por definição. Nenhuma ferramenta externa (spectral, redocly) está no stack — introduzi-la exigiria decisão fora do escopo.
- **Alternatives considered**: Validador standalone (redocly/spectral) — rejeitado por adicionar dependência de toolchain não aprovada (Technology Constraints exigiriam ADR).
- **Nota de ambiente**: o build usa staging do spec em `${java.io.tmpdir}` (antrun) para contornar bug de URI do plugin com o caminho não-ASCII `Área de Trabalho` — nada a fazer, já configurado no pom pai.

## R4. Corrupção do cabeçalho de catalog.yaml (resolve CHK001/CHK012/CHK017/CHK020)

- **Decision**: Pré-condição FR-004 **já satisfeita** — nenhum trabalho de reparo permanece nesta entrega.
- **Rationale**: Verificação no git do `platform-shared-contracts`: a versão commitada (ffb05a7) sempre teve o cabeçalho válido `openapi: 3.1.0`; a corrupção (`UTCOME TESTS` / `12/1openapi: 3.1.0`) era **dano local não commitado** no working tree. O arquivo foi restaurado e `git status` está limpo — o working tree coincide com HEAD. A assunção "corrupção acidental" do spec fica **validada** (nunca houve versão corrompida no histórico).
- **Alternatives considered**: Recuperação de conteúdo perdido via histórico — desnecessária: nada além do cabeçalho fora danificado, e o estado commitado é íntegro.

## R5. Versionamento do artefato

- **Decision**: Manter `0.1.0-SNAPSHOT`; sem bump, sem publicação remota.
- **Rationale**: ADR 0005 (semver + coexistência de SNAPSHOT) e ADR 0006 (CI build-only, publicação adiada): mudanças aditivas em fase SNAPSHOT não exigem bump; consumo é via `mvn -B install` local.
- **Alternatives considered**: Bump para 0.2.0 — rejeitado: nenhum consumidor publicado depende do artefato; bump criaria dessincronia com os poms dos serviços que fixam `0.1.0-SNAPSHOT`.

## R6. Documentação da canonicidade no próprio contrato (resolve CHK002)

- **Decision**: A entrada de path carrega `summary` e `description` explicitando que é o endereço canônico de leitura de saldo e que a operação GET será declarada em task subsequente (T-001-2).
- **Rationale**: O contrato é a fonte de verdade lida pelos consumidores (US1); justificativa apenas no spec deste repo seria invisível para os demais 11 repos.
- **Alternatives considered**: Path "nu" sem descrição — rejeitado: um path vazio e mudo pareceria lixo/acidente para um leitor do contrato e convidaria remoção indevida.

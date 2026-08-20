# Quickstart: Validar as referências a ProblemDetail nos desfechos 400/403/404 de addProductImage

Guia de validação executável para `T-002-7-4`/`T-002-7-5`/`T-002-7-6` (ver [data-model.md](./data-model.md), [spec.md](./spec.md)).

## Pré-requisitos

- Nenhum serviço precisa estar rodando — a mudança é só no contrato.
- **Diretório de partida assumido abaixo: a worktree desta feature**, `.agents/worktrees/t-002-7-product-image-errors` (não a raiz de `hb-catalog-service`). `platform-shared-contracts` é um repositório irmão de `hb-catalog-service`, mas a worktree fica três níveis mais funda — por isso os comandos abaixo usam `../../../../platform-shared-contracts` (quatro níveis), não o `../platform-shared-contracts` (um nível) usado por `013`/`014`, que rodaram na raiz de `hb-catalog-service`.

## Validação via build (caminho principal)

```bash
# A partir da raiz da worktree desta feature:
( cd ../../../../platform-shared-contracts && mvn -B -DskipTests install )

find ../../../../platform-shared-contracts/contracts-catalog/target/generated-sources/openapi \
  -type f -path '*/com/hubinity/contracts/catalog/dto/*.java' | wc -l

mvn -B verify
```

**Resultado esperado**: build do contrato conclui sem erro; contagem de DTOs permanece estável; `mvn -B verify` conclui verde — trivialmente, já que `addProductImage` não tem implementação Java (nenhum código é exercitado por essa operação hoje).

## Validação manual do diff

```bash
git -C ../../../../platform-shared-contracts diff contracts-catalog/openapi/catalog.yaml
```

**Resultado esperado**: apenas linhas `+` nos três blocos `400`/`403`/`404` de `addProductImage`; nenhuma linha `-`.

## Validação de cada desfecho individualmente

```bash
# Confirma que os três desfechos têm content referenciando ProblemDetail,
# e que nenhum outro desfecho da operação (201) foi tocado.
sed -n '/operationId: addProductImage/,/^components:/p' \
  ../../../../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml
```

**Resultado esperado**: os blocos `400`, `403` e `404` mostram `content.application/json.schema.$ref: '#/components/schemas/ProblemDetail'` cada um, com a `description` original preservada; o bloco `201` permanece referenciando `ProductImageResponse`, sem `content` adicional.

## Fora do escopo desta validação

- Qualquer desfecho de erro de outras operações do documento (`getProductById` 404 — `013`; `getStockItemByProductId` 404 — `014`).
- A implementação da operação `addProductImage` (cadeias `T-002`/`T-003`) — não exercitada aqui.

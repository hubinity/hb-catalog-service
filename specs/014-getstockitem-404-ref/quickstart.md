# Quickstart: Validar a referência a ProblemDetail no 404 de getStockItemByProductId

Guia de validação executável para `T-002-7-3` (ver [data-model.md](./data-model.md), [spec.md](./spec.md)).

## Pré-requisitos

- Nenhum serviço precisa estar rodando — a mudança é só no contrato.
- **Diretório de partida assumido abaixo: raiz de `hb-catalog-service`.** `platform-shared-contracts` é um repositório irmão — os comandos usam `../platform-shared-contracts` explicitamente.

## Validação via build (caminho principal)

```bash
# A partir da raiz de hb-catalog-service:
( cd ../platform-shared-contracts && mvn -B -DskipTests install )

find ../platform-shared-contracts/contracts-catalog/target/generated-sources/openapi \
  -type f -path '*/com/hubinity/contracts/catalog/dto/*.java' | wc -l

mvn -B verify
```

**Resultado esperado**: build do contrato conclui sem erro; contagem de DTOs permanece 7; `mvn -B verify` conclui verde — trivialmente, já que `getStockItemByProductId` não tem implementação Java (nenhum código é exercitado por essa rota hoje).

## Validação manual do diff

```bash
git -C ../platform-shared-contracts diff contracts-catalog/openapi/catalog.yaml
```

**Resultado esperado**: apenas linhas `+` no bloco `404` de `getStockItemByProductId`; nenhuma linha `-`.

## Fora do escopo desta validação

- Os três desfechos de erro de `addProductImage` (400/403/404) — pertencem a `T-002-7-4..6`.
- A implementação da rota `getStockItemByProductId` (cadeia `T-004`) — não exercitada aqui.

# Quickstart: Validar a referência a ProblemDetail no 404 de getProductById

Guia de validação executável para `T-002-7-2` (ver [data-model.md](./data-model.md) para o desenho exato, [spec.md](./spec.md) para os requisitos).

## Pré-requisitos

- Nenhum serviço precisa estar rodando — a mudança é só no contrato (`platform-shared-contracts`).
- **Diretório de partida assumido abaixo: raiz de `hb-catalog-service`** (onde este `quickstart.md` vive). `platform-shared-contracts` é um repositório **irmão**, não um subdiretório — os comandos usam `../platform-shared-contracts` explicitamente (achado do `/speckit-analyze`, H1; mesma convenção já em `hb-catalog-service/CLAUDE.md`, seção Commands).

## Validação via build (caminho principal)

```bash
# 0. A partir da raiz de hb-catalog-service:
# 1. Build do módulo de contratos (repo irmão) — valida OpenAPI 3.1 e gera ProblemDetail.java (já existente desde T-002-7-1)
( cd ../platform-shared-contracts && mvn -B -DskipTests install )

# 2. Confirmar que a contagem de DTOs gerados permanece 7 (nenhum schema novo, nenhuma remoção)
find ../platform-shared-contracts/contracts-catalog/target/generated-sources/openapi \
  -type f -path '*/com/hubinity/contracts/catalog/dto/*.java' | wc -l

# 3. Reinstalar dependência (passo 1) já deixa o jar disponível; confirmar regressão zero no consumidor, ainda na raiz de hb-catalog-service:
mvn -B verify
```

**Resultado esperado**: passo 1 conclui sem erro; passo 2 mostra 7 arquivos (mesma contagem de `T-002-7-1`); passo 3 conclui verde, sem nenhum teste alterado.

## Validação manual do diff

```bash
# platform-shared-contracts é seu próprio repositório git (repo irmão) — use -C, não cd,
# para rodar o diff nele a partir da raiz de hb-catalog-service:
git -C ../platform-shared-contracts diff contracts-catalog/openapi/catalog.yaml
```

**Resultado esperado**: apenas linhas `+` no bloco `404` de `getProductById` (o novo `content`); nenhuma linha `-`; a `description` "Product not found" aparece inalterada no diff (fora do hunk, ou presente sem marca de mudança).

## Fora do escopo desta validação

Os outros quatro desfechos de erro do documento (`getStockItemByProductId` 404, `addProductImage` 400/403/404) permanecem `description`-only e não são exercitados aqui — pertencem a `T-002-7-3` até `T-002-7-6`.

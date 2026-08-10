# Data Model: Requisito de autorização da operação de leitura de saldo (T-001-5)

**Date**: 2026-07-24 · **Plan**: [plan.md](./plan.md)

Nenhuma entidade de banco. O "modelo de dados" é a estrutura de segurança do documento de contrato.

## Entidade 1 — Security Scheme `bearerAuth` (novo)

| Campo | Valor |
|---|---|
| Localização | `components/securitySchemes/bearerAuth` |
| `type` | `http` |
| `scheme` | `bearer` |
| `bearerFormat` | `JWT` (documentacional) |
| `description` | Identifica o emissor (Keycloak realm `hibit`); sem URL de ambiente |
| Regras | FR-001; primeiro securityScheme do documento |

## Entidade 2 — Requisito `security` global (novo)

| Aspecto | Valor |
|---|---|
| Localização | Nível raiz do documento (fora de `paths`/`components`) |
| Valor | `- bearerAuth: []` (array de scopes vazio — HTTP bearer não usa scopes) |
| Alcance | Todas as operações (products, stock) herdam |
| Regras | FR-002; FR-003 (operação de saldo não sobrescreve) |

## Entidade 3 — Path Item do saldo (existente — description finalizada)

| Aspecto | Valor |
|---|---|
| Mudança | `description` reescrita para o estado final (operação, parâmetro, corpo e autorização declarados; cadeia T-001 completa) |
| Intacto | `summary`, operação `get`, parâmetro, respostas |

## Entidade 4 — `SecurityConfig.java` (serviço — fonte de verdade, intocado)

Regra efetiva: `/api/**` → `authenticated()`; reads sem `@PreAuthorize`. Fundamenta a escolha "bearerAuth global, sem role".

## Invariantes do documento após a edição

1. `components` ganha `securitySchemes` (antes ausente); `schemas` intocado.
2. Bloco `security` presente no nível raiz (antes ausente).
3. Nenhuma operação declara `security` próprio (todas herdam o global) — FR-003.
4. Paths, operações, parâmetros, tags e schemas byte-a-byte intactos exceto a description do Path Item de saldo (FR-005).
5. Documento parseável; DTOs inalterados (segurança não afeta geração de modelos — R5).

## Estado da cadeia T-001-x após esta entrega — COMPLETA

```
com-corpo-de-resposta (T-001-4, done)
  → com-autorização (T-001-5, esta feature)   ← FECHA A CADEIA
```

Operação `getStockItemByProductId` integralmente especificada: endereço (T-001-1), verbo (T-001-2), parâmetro (T-001-3), corpo de resposta (T-001-4), autorização (T-001-5).

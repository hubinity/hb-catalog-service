# Research: Especificação fina do parâmetro productId (T-001-3)

**Date**: 2026-07-23 · **Plan**: [plan.md](./plan.md)

Escopo residual — as incertezas estruturais foram resolvidas pela cadeia (features 001/002). Nenhum NEEDS CLARIFICATION permanece.

## R1. Description do parâmetro

- **Decision**: `description: Product UUID` — texto idêntico ao do parâmetro de `getProductById`.
- **Rationale**: Paridade exata com a única convenção existente; o parâmetro identifica o mesmo conceito (UUID do produto). Nenhuma informação semântica extra (404, existência de saldo) entra na description — já registrada na resposta (feature 002, R4).
- **Alternatives considered**: "Product UUID (path key of the stock sub-resource)" (rejeitado — verboso e sem paralelo na convenção); duplicar semântica do 404 (rejeitado — violaria a regra de não-duplicação do spec).

## R2. Ratificação do bloco da contingência

- **Decision**: O bloco entregue pela contingência FR-006 (feature 002) é ratificado como definição oficial sem alteração estrutural; a verificação de paridade campo a campo integra o quickstart, e a evidência vai no corpo do commit (FR-002).
- **Rationale**: Comparação real (2026-07-23): bloco da contingência = `name/in/required/schema(string,uuid)`; convenção `getProductById` = mesmos campos + `description`. Única diferença: description ausente — exatamente o entregável de R1. Nenhuma correção estrutural necessária.
- **Alternatives considered**: Reescrever o bloco do zero (rejeitado — churn sem mudança semântica, diff maior que o necessário).

## R3. Texto da description do Path Item

- **Decision**: Texto fixado em FR-003 (aprovado na revisão do checklist, CHK002): "…The GET operation and its productId parameter are declared; the response body and authorization are declared by follow-up contract tasks (T-001-4, T-001-5)."
- **Rationale**: O texto atual cita o parâmetro como pendente (T-001-3..T-001-5) — factualmente incorreto após esta entrega. Padrão de progressão estabelecido pela feature 002 (R5): a description acompanha o estado real da cadeia.
- **Alternatives considered**: Manter intacta (rejeitado — mentiria sobre o estado); remover menção às pendências (rejeitado — consumidores perderiam o aviso de corpo/auth pendentes).

## R4. Risco de build

- **Decision**: Nenhuma contingência nova. Adicionar `description` a um parâmetro é inócuo para o swagger-parser; se o build falhar (altamente improvável), o diff de 1 linha é revertível trivialmente e a falha indicaria problema externo à mudança.
- **Rationale**: A classe de erro da cadeia (parâmetro não declarado) foi resolvida na feature 002; description é campo documentacional sem efeito de validação.
- **Alternatives considered**: Definir contingência formal (rejeitado — sem modo de falha plausível causado pela mudança).

## R5. Herança de autoridade de validação e workflow

- **Decision**: Sem mudanças — build do módulo como gate (feature 001, R3); branch `feature/stock-balance-path`; commits no polish (contrato + artefatos, padrão T008/T011 da feature 002).
- **Rationale**: Padrão estável da cadeia; reabrir seria retrabalho.
- **Alternatives considered**: n/a (decisão herdada).

# 🤝 Guia de Contribuição — hb-catalog-service

Microsserviço de catálogo do ecossistema **Hubinity** (marca HiBit).
Leia este guia antes de abrir qualquer Pull Request.

---

## 1. Pré-requisitos

- JDK 21 (Temurin recomendado)
- Maven 3.9.x
- Docker + Docker Compose (Testcontainers e stack local via `platform-infra`)
- Acesso ao repositório `hubinity/hb-catalog-service`

### Setup inicial

```bash
# 1. Buildar os contratos compartilhados (uma vez por máquina, ou após mudanças)
( cd ../platform-shared-contracts && mvn -B -DskipTests install )

# 2. Confirmar que o build passa
mvn -B verify
```

---

## 2. Fluxo de Trabalho

### 2.1 Crie uma branch a partir de `main`

```bash
git checkout main && git pull
git checkout -b feature/nome-da-funcionalidade   # ou fix/nome-do-bug
```

> ⚠️ **Nunca trabalhe diretamente na branch `main`.** Push direto é bloqueado.

### 2.2 Desenvolva e faça commits seguindo o padrão (seção 3)

### 2.3 Rode os quality gates antes de abrir o PR

```bash
# Obrigatório sempre
mvn -B verify

# Obrigatório se você tocou em persistência, migrations, outbox ou messaging
mvn -P integration-tests verify   # requer Docker daemon ativo
```

### 2.4 Abra um Pull Request para `main`

- Preencha o template (seção 4).
- O CODEOWNERS (`.github/CODEOWNERS`) atribui os revisores automaticamente
  (`@hubinity/backend` para código Java, `@hubinity/devops` para Dockerfile/CI).
- Merge somente após aprovação — merge sem review é bloqueado.

---

## 3. Padrão de Mensagens de Commit

Formato: `tipo: descrição curta no presente` (escopo opcional: `feat(catalog): …`).

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `docs` | Alteração em documentação |
| `style` | Formatação, sem mudança de lógica |
| `refactor` | Refatoração de código |
| `test` | Adição ou correção de testes |
| `chore` | Manutenção (CI, CODEOWNERS, deps) |

**Exemplos reais do histórico:**

```
feat(catalog): stock endpoints + reservation saga + outbox pattern
fix(catalog): close subcategory/reparent race in CategoryService delete guard
docs: add outbox design documentation
chore: add CODEOWNERS to define backend reviewers
```

---

## 4. Template de Pull Request

```
## O que foi feito
Descreva brevemente o que foi implementado.

## Como testar
1. Passo 1
2. Passo 2

## Checklist
- [ ] `mvn -B verify` passa
- [ ] `mvn -P integration-tests verify` passa (se toquei persistência/messaging)
- [ ] Migrations novas são aditivas (nunca editei migration já aplicada)
- [ ] Não quebrei funcionalidade existente
- [ ] Os commits seguem o padrão definido
```

---

## 5. Regras Específicas deste Serviço

Antes de codar, leia `CLAUDE.md` (visão de arquitetura) e os ADRs em `docs/adr/`. Resumo do que é inegociável:

- **Flyway only** (`ddl-auto: validate`) — mudança de schema = nova migration numerada em `src/main/resources/db/migration/`. Nunca edite uma migration já aplicada.
- **Concorrência de estoque** via `UPDATE … WHERE available >= :qty` condicional — não introduza `SELECT … FOR UPDATE` em novos checks.
- **Eventos** sempre via Transactional Outbox (`EventPublisher` dentro da TX de negócio) — nunca `rabbitTemplate.send()` direto no service. Receita completa em `README-outbox.md`.
- **`Idempotency-Key`** é obrigatório nos endpoints mutantes de estoque — novos endpoints mutantes de estoque devem ser adicionados aos patterns do `IdempotencyFilter`.
- **Erros** sempre como RFC 7807 ProblemDetail: crie exceção tipada em `api/error/` e trate no `ApiExceptionHandler`.
- ❌ Não suba artefatos de build (`target/`, `.class`, `.jar`) nem credenciais em YAML — config sensível só via variável de ambiente.

---

## 6. Dúvidas?

Abra uma issue ou pergunte no canal do time antes de fazer algo que não tem certeza.
É melhor perguntar do que precisar desfazer um merge. 😉

# 🌿 Estratégia de Branches — Café & Parafuso

Este documento descreve como o versionamento é organizado em todos os repositórios do projeto.

---

## Visão Geral

```
main  ────────────────────────────────────────────────── (produção)
        ↑ merge aprovado pelo orientador
develop ──────────────────────────────────────────────── (integração)
        ↑ PR aprovado pelo orientador
feature/nome-da-funcionalidade  (branch do aluno)
fix/nome-do-bug                 (branch do aluno)
```

---

## Regras por Branch

### `main`
- Código **estável e funcional**
- Somente o orientador realiza merge
- Representa a entrega oficial do sistema

### `develop`
- Branch de **integração contínua**
- Recebe os Pull Requests dos alunos
- Somente o orientador aprova e faz merge

### `feature/*`
- Criada pelo aluno para **cada nova funcionalidade**
- Parte sempre de `develop`
- Nomeação: `feature/nome-descritivo` (sem acentos, sem espaços)
- Exemplos:
  - `feature/tela-login`
  - `feature/listagem-produtos`
  - `feature/gerar-pedido`

### `fix/*`
- Criada pelo aluno para **correção de bugs**
- Parte de `develop`
- Exemplos:
  - `fix/calculo-total-incorreto`
  - `fix/botao-nao-responde`

---

## Ciclo de Vida de uma Branch

```
1. Aluno cria branch a partir de develop
   git checkout develop
   git checkout -b feature/minha-funcionalidade

2. Aluno desenvolve e commita
   git add .
   git commit -m "feat: descrição"

3. Aluno sobe para o fork
   git push origin feature/minha-funcionalidade

4. Aluno abre Pull Request → develop do repositório oficial

5. Orientador revisa o código

6. Orientador aprova e faz o merge em develop

7. Branch do aluno pode ser deletada após o merge
```

---

## Proteções Configuradas no Repositório

| Branch | Push direto | Merge sem aprovação |
|---|---|---|
| `main` | ❌ Bloqueado | ❌ Bloqueado |
| `develop` | ❌ Bloqueado | ❌ Bloqueado |
| `feature/*` | ✅ Permitido (apenas no fork) | — |
| `fix/*` | ✅ Permitido (apenas no fork) | — |

> As proteções são configuradas pelo orientador nas **Settings > Branches** do repositório.

---

## Dica: Sempre sincronize antes de começar

```bash
# Atualiza seu develop local com o repositório oficial
git fetch upstream
git checkout develop
git merge upstream/develop

# Cria a nova branch a partir do develop atualizado
git checkout -b feature/nova-funcionalidade
```

Trabalhar com código desatualizado gera conflitos desnecessários.
Sincronize **sempre** antes de começar algo novo.

# 🤝 Guia de Contribuição

Bem-vindo ao projeto **Café & Parafuso**!
Leia este guia com atenção antes de fazer qualquer alteração no repositório.

---

## 1. Pré-requisitos

- Conta no GitHub criada
- Git instalado na máquina
- Repositório original **forkado** para sua conta

---

## 2. Fluxo de Trabalho (Passo a Passo)

### 2.1 Faça o fork do repositório oficial
No GitHub, acesse o repositório do professor e clique em **Fork**.

### 2.2 Clone o seu fork localmente
```bash
git clone https://github.com/SEU_USUARIO/cp-product-catalog.git
cd cp-product-catalog
```

### 2.3 Configure o repositório original como `upstream`
```bash
git remote add upstream https://github.com/USUARIO_PROFESSOR/cp-product-catalog.git
```

### 2.4 Crie uma branch para sua tarefa
```bash
# Formato obrigatório
git checkout -b feature/nome-da-funcionalidade

# Exemplos
git checkout -b feature/tela-produto
git checkout -b fix/calculo-total
```

> ⚠️ **Nunca trabalhe diretamente na branch `main` ou `develop`.**

### 2.5 Desenvolva e faça commits
```bash
git add .
git commit -m "feat: adiciona tela de listagem de produtos"
```

Siga o padrão de mensagens abaixo (seção 4).

### 2.6 Suba sua branch para o seu fork
```bash
git push origin feature/nome-da-funcionalidade
```

### 2.7 Abra um Pull Request (PR)
1. Acesse seu fork no GitHub
2. Clique em **Compare & pull request**
3. Direcione o PR para a branch `develop` do repositório do professor
4. Preencha o título e a descrição conforme o template (seção 5)
5. Aguarde a revisão

---

## 3. Estrutura de Branches

| Branch | Finalidade | Quem gerencia |
|---|---|---|
| `main` | Versão estável e entregável | Professor (orientador) |
| `develop` | Integração das funcionalidades | Professor (orientador) |
| `feature/nome` | Desenvolvimento de nova funcionalidade | Aluno |
| `fix/nome` | Correção de bug | Aluno |

---

## 4. Padrão de Mensagens de Commit

Use o formato: `tipo: descrição curta no presente`

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `docs` | Alteração em documentação |
| `style` | Formatação, sem mudança de lógica |
| `refactor` | Refatoração de código |
| `test` | Adição ou correção de testes |

**Exemplos:**
```
feat: adiciona botão de pagamento via Pix
fix: corrige cálculo do total do carrinho
docs: atualiza instruções de execução no README
```

---

## 5. Template de Pull Request

Ao abrir um PR, use a estrutura abaixo na descrição:

```
## O que foi feito
Descreva brevemente o que foi implementado.

## Como testar
1. Passo 1
2. Passo 2

## Checklist
- [ ] O código compila sem erros
- [ ] Testei manualmente
- [ ] Não quebrei funcionalidade existente
- [ ] O commit segue o padrão definido
```

---

## 6. Regras Gerais

- ❌ Não suba arquivos `.class`, `.jar` ou pastas de build
- ❌ Não commite senhas, tokens ou dados pessoais
- ❌ Não faça merge sem aprovação do orientador
- ✅ Mantenha o `.gitignore` atualizado
- ✅ Sempre sincronize com o `upstream` antes de começar

### Sincronizar com o repositório oficial
```bash
git fetch upstream
git checkout develop
git merge upstream/develop
```

---

## 7. Dúvidas?

Fale com o orientador antes de fazer algo que não tem certeza.
É melhor perguntar do que precisar desfazer um commit. 😉

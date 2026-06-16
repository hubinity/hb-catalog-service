# 🛒 Sistema de Catálogo de Produtos — HiBit

> Parte do ecossistema **HiBit** · Desenvolvido por alunos do Curso Técnico em Informática

---

## 📌 Sobre o Projeto

O Sistema de Catálogo de Produtos é responsável por **centralizar todas as informações sobre os itens vendidos** pelo snack-bar HiBit. Ele funciona como a “vitrine digital” do negócio: é aqui que cada produto é cadastrado, atualizado e disponibilizado para os demais sistemas do ecossistema. <br><br>
Pense neste sistema como o **“depósito de informações” sobre tudo o que a lanchonete vende**. Sem ele, os outros sistemas não saberiam quais produtos existem, quanto custam ou se estão disponíveis.

**Este sistema se integra com:**
- [X] Sistema de Carrinho de Compras
- [X] Sistema Principal (HiBit)
- [ ] Sistema de Controle de Caixa

---

## 🚀 Escopo Funcional (Alto Nível)

- [ ] Cadastro de Produtos (CRUD completo)
- [ ] Gerenciamento de Categorias
- [ ] Controle de Disponibilidade
- [ ] Exposição de Dados (API)

---

## 🛠️ Tecnologias

| Camada | Tecnologia | Versão Recomendada |
|---|---|---|
| Linguagem | Java | 21+ |
| Framework | Spring Boot | 4.1.0 |
| Banco de Dados | PostgreSQL | 18 |
| ORM | Spring Data JPA / Hibernate | - |
| Build Tool | Maven | 3.9.16 |
| FrontEnd | Angular + TailwindCSS | 22 + 4 |
| Servidor | Tomcat Embedded (Spring Boot) | - |
| Controle de versão | Git + GitHub | - |

---

## 📁 Estrutura do Projeto

```
hb-product-catalog/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── br/com/cp-product-catalog/catalog
|   │   │       ├── controller/          ← Controladores (recebem requisições)
|   │   │       ├── service/             ← Regras de negócio
|   │   │       ├── repository/          ← Acesso ao banco de dados
|   │   │       ├── model/               ← Entidades (Produto, Categoria)
|   │   │       └── dto/                 ← Objetos de transferência de dados
|   |   └── resources/
│   │       ├── static/                  ← Arquivos estáticos (CSS, JS)
│   │       ├── templates/               ← Páginas Thymeleaf (HTML)
│   │       └── application.properties   ← Configurações do sistema
│   └── test/
├── docs/
├── .gitignore
├── CONTRIBUTING.md
├── pom.xml                              ← Dependências do projeto (Maven)     
└── README.md
```

---

## ▶️ Como Executar

```bash
# 1. Clone o repositório
git clone https://github.com/SEU_USUARIO/cp-product-catalog.git

# 2. Acesse a pasta
cd cp-product-catalog

# 3. Execute o projeto
# (instruções específicas da equipe)
```

---

## 🤝 Como Contribuir

Leia o arquivo [CONTRIBUTING.md](./CONTRIBUTING.md) antes de qualquer alteração.

---

## 👥 Equipe

| Nome | Função |
|---|---|
| Khauan Rodrigues de Oliveira | Desenvolvedor |
| Marllon Victor de Souza Lima | Desenvolvedor |
| Ryan Paiva Alvarenga | Desenvolvedor |
| Samuel Henrique Ferreira Barbosa | Desenvolvedor |
| Samuel Martins Silva | Desenvolvedor |
| Wender Lucas Viana Pereira | Desenvolvedor |

**Orientador:** Lenoln Muniz · [LinkedIn](https://linkedin.com/in/lenoln-io)

---

## 📄 Licença

Este projeto é de uso educacional, desenvolvido como projeto integrador do Curso Técnico em Informática.

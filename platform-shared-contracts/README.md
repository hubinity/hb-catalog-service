# platform-shared-contracts (Library) - Ecossistema Hubinity - Active
> Parte integrante do ecossistema distribuído Hubinity.

---

## 💻 Visão Geral
- **O que faz:** Single source of truth dos contratos cross-service da Hubinity. Empacota: (1) especificações **OpenAPI 3.1** por bounded context (catalog/order/support/cashier) que geram **DTOs Java Jackson-anotados** em build-time, (2) **JSON Schema 2020-12** para payloads de eventos de domínio (8 eventos), e (3) um sub-projeto npm `@hubinity/tailwind-preset` com os design tokens compartilhados pelas 4 SPAs do ecossistema.
- **Problema que resolve:** Sem este repositório, cada serviço duplicaria DTOs (drift de tipos), cada frontend redeclararia tokens visuais (drift de marca) e contratos de evento ficariam implícitos no código de quem publica. Aqui, o contrato é o artefato versionado — quem viola contrato quebra o build.
- **Posicionamento no Ecossistema:** Camada de **biblioteca compilada** consumida via Maven (JARs) pelos 4 backends Spring Boot e via npm (preset) pelas 4 SPAs Angular. Não roda em runtime; é uma dependência de build.

## 🏗️ Papel na Arquitetura
- **Tipo de Componente:** Biblioteca multi-módulo (Maven parent pom + 5 sub-módulos) + sub-projeto npm independente.
- **Responsabilidades Principais:**
  - Gerar DTOs Java a partir das specs OpenAPI (modo `models-only`, sem stubs de servidor — ver ADR 0002).
  - Gerar POJOs Java a partir dos JSON Schemas de eventos (jsonschema2pojo, ver ADR 0004).
  - Compilar e distribuir o preset Tailwind 4 com os design tokens (light/dark + variant `totem`).
  - Manter políticas de versionamento SemVer e coexistência paralela (ver ADR 0005).
- **Limites e Fronteiras (Boundaries):**
  - **Não** contém lógica de negócio.
  - **Não** gera server stubs (interfaces de Controller) — cada serviço implementa seus endpoints manualmente, importando apenas o DTO.
  - **Não** publica eventos em runtime — apenas define o schema; quem publica é o backend dono do domínio.

## 🔗 Dependências e Comunicação
### Serviços Internos da Hubinity
- **Nenhuma dependência upstream.** É consumido em build-time por:
  - `hb-catalog-service` (consome `contracts-catalog` + `contracts-events`)
  - `hb-support-service` (consome `contracts-support` + `contracts-events`)
  - `hb-cashier-service` (consome `contracts-cashier` + `contracts-events`)
  - `sc-order-service` (consome `contracts-order` + `contracts-events`)
  - `hb-catalog-web`, `hb-support-web`, `hb-cashier-web`, `sc-totem-web` (consomem `@hubinity/tailwind-preset`)

### Infraestrutura e Serviços Externos
- **Maven Central** — resolução de Spring Boot Parent, Jackson, openapi-generator etc.
- **GitHub Packages** (futuro) — destino do `mvn deploy` e `npm publish` (block atualmente comentado no parent pom; ver seção *Publishing*).

## 🛠️ Tecnologias e Ferramentas
| Camada | Tecnologia | Versão |
| :--- | :--- | :--- |
| Linguagem (lib Java) | Java | 21 (LTS) |
| Build (lib Java) | Maven | 3.9.x |
| Parent BOM | Spring Boot (parent) referenciado pelos consumers | 4.1.0 |
| Gerador DTOs OpenAPI | openapi-generator-maven-plugin (`spring`, model-only) | 7.13.0 |
| Gerador POJOs Eventos | jsonschema2pojo-maven-plugin | 1.2.2 |
| Serialização | Jackson (BOM) | 2.18.2 |
| Anotações | Jakarta Annotation API / Jakarta Validation API | 3.0.0 / 3.1.1 |
| Linguagem (preset npm) | TypeScript | 5.6.x |
| Bundler (preset npm) | tsup | 8.3.x |
| Runtime (preset npm) | Node | ≥ 20 |
| Design tokens | Tailwind CSS | 4.x (peerDependency) |

## 📐 Padrões de Projeto e Arquitetura do Código
- **Estilo Arquitetural:** **Contract-First / Schema-First** — o YAML/JSON é a verdade; o código Java é derivado mecanicamente.
- **Padrões Relevantes:**
  - **Multi-module Maven** com parent `<packaging>pom</packaging>` para compartilhar pluginManagement.
  - **Model-only generation** (sem server stubs nem client invokers) — ver ADR 0002.
  - **Event versioning by path** (`events/v1/<EventName>.schema.json`) — ver ADR 0007.
  - **Jakarta EE + Jackson only** (sem Spring Web no generated code) — ver ADR 0003.
  - **SemVer + coexistência** — quando v2.0.0 sair, v1.x.x continua publicado por ≥ 1 ciclo de release (ver ADR 0005).
- **Workaround conhecido:** o openapi-generator quebra com paths que contêm caracteres não-ASCII (ex.: "Área de Trabalho"). Resolvido via cópia automática dos specs para `${java.io.tmpdir}` em fase `initialize` (maven-antrun-plugin) — ver bloco `<properties>` do parent pom.

## 📂 Estrutura do Projeto
```text
platform-shared-contracts/
├── pom.xml                                  # parent (packaging: pom)
├── settings.xml                             # template Maven para GitHub Packages
├── contracts-catalog/
│   ├── pom.xml
│   └── openapi/catalog.yaml                 # Product, Category, StockItem, StockMovement
├── contracts-order/
│   ├── pom.xml
│   └── openapi/order.yaml                   # Order, OrderItem, Payment
├── contracts-support/
│   ├── pom.xml
│   └── openapi/support.yaml                 # Ticket, TicketItem, Customer, Service, Technician
├── contracts-cashier/
│   ├── pom.xml
│   └── openapi/cashier.yaml                 # LedgerEntry, LedgerCategory, CashSession
├── contracts-events/
│   ├── pom.xml
│   ├── VERSIONING.md
│   └── events/v1/
│       ├── OrderPaid.schema.json
│       ├── OrderCancelled.schema.json
│       ├── ServiceRevenueGenerated.schema.json
│       ├── StockChanged.schema.json
│       ├── PriceChanged.schema.json
│       ├── ProductCreated.schema.json
│       ├── ProductUpdated.schema.json
│       └── ProductDeactivated.schema.json
├── tailwind-preset/                         # sub-projeto npm (independente do Maven)
│   ├── package.json                         # @hubinity/tailwind-preset v0.1.0
│   ├── tsconfig.json
│   └── src/
│       ├── index.ts
│       ├── theme.css
│       └── theme.totem.css
├── docs/adr/                                # 8 ADRs (decisões de arquitetura)
└── .github/workflows/ci.yml                 # CI build-only (não publica)
```

Os DTOs gerados aterrissam em `target/generated-sources/openapi/src/main/java/`, sob `com.hubinity.contracts.<domain>.dto` (ou `com.hubinity.contracts.events` para eventos), e são adicionados ao classpath via `build-helper-maven-plugin`.

## ⚙️ Configuração e Variáveis de Ambiente
```bash
# Para o futuro mvn deploy → GitHub Packages (hoje desabilitado;
# o bloco <distributionManagement> no parent pom está comentado).
GITHUB_USERNAME=<seu-user-github>
GITHUB_TOKEN=<PAT com escopo read:packages e write:packages>

# Para o futuro npm publish do tailwind-preset:
# o package.json já define publishConfig.registry = https://npm.pkg.github.com
```

Nenhuma variável é necessária para o build local — `mvn -B clean package -DskipTests` funciona offline depois do primeiro download de dependências.

## 🚀 Como Instalar e Executar
### Pré-requisitos
- JDK 21 (Temurin recomendado)
- Maven 3.9.x
- Node 20+ e npm (apenas para o sub-projeto `tailwind-preset/`)

### Passos para Instalação
```bash
git clone <repo-url> platform-shared-contracts
cd platform-shared-contracts
```

### Build da biblioteca Maven (DTOs Java)
```bash
# Build completo: generate-sources → compile → test → install
mvn clean install

# Build sem testes (mais rápido durante iteração)
mvn -B clean package -DskipTests

# Regenerar apenas as sources sem compilar/testar
mvn -DskipTests generate-sources
```

Após `mvn install`, os artefatos ficam disponíveis no `~/.m2` local:

| Artifact ID         | Coordenadas Maven                                  |
| ------------------- | -------------------------------------------------- |
| contracts-catalog   | `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`    |
| contracts-order     | `com.hubinity:contracts-order:0.1.0-SNAPSHOT`      |
| contracts-support   | `com.hubinity:contracts-support:0.1.0-SNAPSHOT`    |
| contracts-cashier   | `com.hubinity:contracts-cashier:0.1.0-SNAPSHOT`    |
| contracts-events    | `com.hubinity:contracts-events:0.1.0-SNAPSHOT`     |

### Build do preset Tailwind (npm)
```bash
cd tailwind-preset
npm install
npm run build         # gera dist/{index.js,index.cjs,index.d.ts,theme.css,theme.totem.css}
npm pack --dry-run    # confere o conteúdo do tarball publicável
```

### Consumir do lado backend (Spring Boot)
```xml
<dependency>
    <groupId>com.hubinity</groupId>
    <artifactId>contracts-catalog</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>

<!-- Serviços que publicam ou consomem eventos -->
<dependency>
    <groupId>com.hubinity</groupId>
    <artifactId>contracts-events</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Consumir do lado frontend (Angular)
```bash
npm install @hubinity/tailwind-preset
```
```css
/* em styles.css de cada SPA */
@import "@hubinity/tailwind-preset/theme.css";
/* ou para o totem: @import "@hubinity/tailwind-preset/theme.totem.css"; */
```

### Publicação (futuro)
A publicação em **GitHub Packages** ainda **não está habilitada**:
1. Descomentar `<distributionManagement>` em `./pom.xml`.
2. Copiar `./settings.xml` para `~/.m2/settings.xml` (ou mergear o bloco `<servers>`) e exportar `GITHUB_USERNAME`/`GITHUB_TOKEN`.
3. Rodar `mvn -DskipTests deploy`.

Hoje o workflow `ci.yml` apenas roda `mvn verify` — não publica.

## 🔄 Fluxos Principais

### Versionamento (SemVer)
- **Patch (0.0.x)** — adições retrocompatíveis em campos opcionais, ajustes de doc.
- **Minor (0.x.0)** — novos schemas, novos endpoints, novos tipos de evento (retrocompatível).
- **Major (x.0.0)** — breaking changes (remover/renomear campo, mudar tipo, tornar campo required sem default).
- **Política de coexistência:** ao subir um major, a major anterior continua publicada paralelamente por **pelo menos 1 ciclo de release** para permitir migração dos consumers (detalhe em ADR 0005).

### Cookbook: adicionar um novo módulo de contrato
1. Escolher um nome de domínio (ex.: `inventory`).
2. Criar `contracts-inventory/` copiando `contracts-catalog/` como template.
3. Adicionar `<module>contracts-inventory</module>` ao parent `pom.xml`.
4. Criar `contracts-inventory/openapi/inventory.yaml` (ou `events/*.schema.json`).
5. Ajustar `inputSpec`, `modelPackage`, `apiPackage` na execução do openapi-generator.
6. Rodar `mvn -DskipTests generate-sources -pl contracts-inventory` e inspecionar o output.
7. `mvn -B verify` na raiz para confirmar que o build completo continua passando.

## 📊 Observabilidade e Testes
- **Logs & Tracing:** Nenhum mecanismo de tracing/observabilidade próprio nesta camada (biblioteca compilada — observabilidade fica nos consumers em runtime).
- **Como Rodar os Testes:**
  - Java: `mvn test` — valida que a geração de sources roda e que o código gerado compila.
  - npm preset: `cd tailwind-preset && npm pack --dry-run` — valida que o tarball publicável está consistente.

---

## 📚 ADRs (Decisões de Arquitetura)
Consulte `docs/adr/` para o histórico completo:
- **0001** — Multi-module Maven layout
- **0002** — Generate models-only (não server stubs)
- **0003** — Jakarta EE + Jackson only (sem Spring Web no generated code)
- **0004** — jsonschema2pojo para eventos
- **0005** — SemVer SNAPSHOT + coexistência paralela ≥ 1 release
- **0006** — CI build-only (sem publish ainda)
- **0007** — Event versioning by path (`events/v1/`)
- **0008** — Tailwind preset como shared package npm

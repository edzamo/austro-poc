# Ecosistema de Evaluación de Créditos — PoC

Sistema completo para la evaluación y gestión de solicitudes de crédito, compuesto por tres proyectos independientes con arquitecturas bien definidas.

---

## Estructura del Ecosistema

```
austro/
├── mms-ux-credit-evaluation/           # Frontend Angular 21 — Feature-Flat Architecture
├── ms-orchestrator-credit-evaluation/  # Quarkus 3.8 — Arquitectura Hexagonal + PostgreSQL
├── ms-risk-mock-credit-evaluation/     # Quarkus 3.8 — Clean/MVC (mock de bureau de crédito)
└── docker-compose.yml                  # Orquestación completa del ecosistema
```

---

## Diagramas de Arquitectura

### 1. Vista General del Ecosistema

```mermaid
flowchart TB
    User(("Usuario"))

    subgraph Ecosystem["Ecosistema de Evaluación de Créditos"]
        direction TB

        subgraph FE["Frontend  ·  :4200"]
            UX["mms-ux-credit-evaluation\nAngular 21  ·  Feature-Flat Architecture\ncredit-evaluation/ → model · service · components"]
        end

        subgraph ORC["Orquestador  ·  :8080"]
            ORCH["ms-orchestrator-credit-evaluation\nQuarkus 3.8  ·  Java 21\nArquitectura Hexagonal (Ports & Adapters)"]
        end

        subgraph MOCK["Mock de Riesgo  ·  :8081"]
            RISK["ms-risk-mock-credit-evaluation\nQuarkus 3.8  ·  Java 21\nClean / MVC por capas"]
        end

        subgraph DB_BOX["Persistencia  ·  :5433"]
            DB[("PostgreSQL 16\ncredit_evaluation_db\nFlyway · Panache · Hibernate ORM")]
        end
    end

    User -- "HTTP / Navegador" --> UX
    UX -- "POST /v1/credit-evaluations\nGET  /v1/credit-evaluations" --> ORCH
    ORCH -- "GET /v1/risk-score/{cedula}  ⏱ 2s" --> RISK
    ORCH -- "GET /v1/customer-debts/{cedula}  ⏱ 1.5s" --> RISK
    ORCH -- "INSERT / SELECT" --> DB
```

---

### 2. Arquitectura Hexagonal — ms-orchestrator-credit-evaluation

```mermaid
flowchart LR
    subgraph AE["Adaptadores de Entrada\ninfrastructure/rest"]
        REST["CreditEvaluationResource\n@Path /v1/credit-evaluations\nJakarta REST · RESTEasy Reactive"]
    end

    subgraph HEX["Hexágono"]
        direction TB

        subgraph PI["Puertos de Entrada\napplication/port/in"]
            P1["«interface»\nEvaluateCreditUseCase"]
            P2["«interface»\nGetEvaluationsUseCase"]
        end

        subgraph UC["Casos de Uso\napplication/usecase"]
            UC1["EvaluateCreditUseCaseImpl\n@Slf4j · @RequiredArgsConstructor"]
            UC2["GetEvaluationsUseCaseImpl"]
        end

        subgraph SVC["Servicios de Aplicación\napplication/service"]
            S1["CedulaValidatorService\nMódulo 10 Ecuador"]
            S2["CreditApprovalService\nscore › 70  ·  cuota ‹ salario×40%"]
        end

        subgraph DOM["Dominio\ndomain/model"]
            M1["CreditEvaluation\n(record)"]
            M2["RiskData + Debt\n(records)"]
            M3["EvaluationStatus\n(enum)"]
        end

        subgraph PO["Puertos de Salida\napplication/port/out"]
            PO1["«interface»\nCreditEvaluationRepository"]
            PO2["«interface»\nRiskServicePort"]
        end
    end

    subgraph AS["Adaptadores de Salida\ninfrastructure"]
        JPA["CreditEvaluationRepositoryImpl\ninfrastructure/persistence\nPanache · PostgreSQL"]
        HTTP["RiskServiceAdapter\ninfrastructure/client\n@RegisterRestClient"]
    end

    REST -->|llama| P1
    REST -->|llama| P2
    P1 -.->|implementado por| UC1
    P2 -.->|implementado por| UC2
    UC1 --> S1
    UC1 --> S2
    UC1 --> PO1
    UC1 --> PO2
    UC2 --> PO1
    UC1 -.- M1
    UC1 -.- M2
    PO1 -.->|implementado por| JPA
    PO2 -.->|implementado por| HTTP
```

---

### 3. Arquitectura Feature-Flat — mms-ux-credit-evaluation

```mermaid
flowchart TB
    subgraph APP["src/app/"]
        subgraph RT["Raíz"]
            AC["app.component.ts\nShell con router-outlet"]
            CF["app.config.ts\nprovideHttpClient · provideRouter"]
            AR["app.routes.ts\nLazy-load del formulario"]
        end

        subgraph FE["credit-evaluation/  ← Feature"]
            MDL["credit-evaluation.model.ts\nCreditEvaluation · CreditEvaluationCommand\nEvaluationStatus"]
            SVC["credit-evaluation.service.ts\nHttpClient · submit() · findAll()"]

            subgraph UI1["credit-evaluation-form/"]
                F1["credit-evaluation-form.component.ts\nSignals · ReactiveForm · FormState"]
                F2["credit-evaluation-form.component.html"]
                F3["credit-evaluation-form.component.scss"]
            end

            subgraph UI2["evaluation-list/"]
                L1["evaluation-list.component.ts\n@Input evaluations · @Input loading"]
                L2["evaluation-list.component.html"]
                L3["evaluation-list.component.scss"]
            end
        end
    end

    AR -->|"lazy import"| F1
    F1 -->|"inject"| SVC
    F1 -->|"usa"| MDL
    F1 -->|"incluye"| L1
    L1 -->|"usa"| MDL
    SVC -->|"HTTP"| MDL
```

---

### 4. Arquitectura por Capas — ms-risk-mock-credit-evaluation

```mermaid
flowchart TB
    subgraph INF["infrastructure/rest"]
        R["RiskResource\n@Path /v1\n@RequiredArgsConstructor"]
        RD["DTOs (records)\nRiskScoreResponseDto\nCustomerDebtsResponseDto"]
    end

    subgraph APP["application/service"]
        SVC["RiskService\n@ApplicationScoped\nGeneración aleatoria con latencia simulada"]
    end

    subgraph DOM["domain/model"]
        M1["RiskScore (record)"]
        M2["CustomerDebt (record)"]
        M3["CustomerDebtsResult (record)"]
    end

    R -->|"llama"| SVC
    SVC -->|"retorna"| M1
    SVC -->|"retorna"| M3
    M3 -->|"contiene"| M2
    R -->|"mapea a"| RD
```

---

### 4. Secuencia — Flujo Completo de Evaluación

```mermaid
sequenceDiagram
    actor Usuario
    participant UX  as Angular :4200
    participant ORC as Orquestador :8080
    participant RSK as Risk Mock :8081
    participant DB  as PostgreSQL :5433

    Usuario->>UX: Llena formulario\n{cédula, monto, años, salario}
    UX->>ORC: POST /v1/credit-evaluations

    ORC->>ORC: Valida cédula\n(Módulo 10 Ecuador)

    par Llamadas paralelas
        ORC->>RSK: GET /v1/risk-score/{cedula}
        RSK-->>ORC: {score: 0-100}  ⏱ ~2s
    and
        ORC->>RSK: GET /v1/customer-debts/{cedula}
        RSK-->>ORC: {debts: [...]}  ⏱ ~1.5s
    end

    ORC->>ORC: Regla de negocio\nscore > 70\nAND (Σdeudas + cuota) < salario × 0.40

    ORC->>DB: INSERT credit_evaluations\n(Flyway + Panache)
    DB-->>ORC: {id, ...}

    ORC-->>UX: HTTP 201 {id, finalStatus, cedula, ...}
    UX-->>Usuario: APROBADO ✅ / RECHAZADO ❌
```

---

## Descripción de los Componentes

### `mms-ux-credit-evaluation` — Frontend Angular 21
**Arquitectura:** Feature-Flat — todo bajo `credit-evaluation/` sin capas intermedias
- `credit-evaluation.model.ts` — interfaces y tipos puros
- `credit-evaluation.service.ts` — HttpClient directo, `submit()` y `findAll()`
- Formulario con validaciones reactivas (cédula 10 dígitos, montos positivos)
- Estado manejado con Angular Signals (`idle | loading | success | error`)
- Visualización del resultado: APROBADO (verde) / RECHAZADO (rojo)
- Historial de evaluaciones en tabla con lazy-load al iniciar
- Puerto: `http://localhost:4200`

### `ms-orchestrator-credit-evaluation` — Microservicio Orquestador
**Arquitectura:** Hexagonal (Puertos y Adaptadores)
- Valida la cédula con algoritmo Módulo 10 de Ecuador
- Consulta en paralelo el score y las deudas al servicio de riesgo
- Aplica la regla de negocio de aprobación
- Persiste el resultado en PostgreSQL con Flyway + Panache
- Puerto: `http://localhost:8080`

### `ms-risk-mock-credit-evaluation` — Mock de Servicio de Riesgo
**Arquitectura:** Clean/MVC por capas
- Genera scores aleatorios (0-100) con latencia de 2 segundos
- Genera deudas aleatorias (0-3) con latencia de 1.5 segundos
- Puerto: `http://localhost:8081`

---

## Inicio Rápido (Docker Compose)

### Prerrequisitos
- Docker 24+
- Docker Compose v2+

### 1. Compilar los microservicios Quarkus
```bash
# Microservicio de riesgos
cd ms-risk-mock-credit-evaluation
./mvnw package -DskipTests
cd ..

# Microservicio orquestador
cd ms-orchestrator-credit-evaluation
./mvnw package -DskipTests
cd ..
```

### 2. Levantar el ecosistema completo
```bash
docker-compose up --build
```

Los servicios se inician en este orden (gestionado por `depends_on` + healthchecks):
1. PostgreSQL
2. ms-risk-mock-credit-evaluation
3. ms-orchestrator-credit-evaluation
4. mms-ux-credit-evaluation

### 3. Acceder a los servicios
| Servicio | URL |
|---|---|
| Frontend Angular | http://localhost:4200 |
| API Orquestador | http://localhost:8080 |
| Swagger Orquestador | http://localhost:8080/swagger-ui |
| API Mock de Riesgo | http://localhost:8081 |
| Swagger Mock de Riesgo | http://localhost:8081/swagger-ui |

---

## Inicio en Modo Desarrollo (sin Docker)

### PostgreSQL local
```bash
# Opción rápida: usar el compose dedicado (recomendado si ya tienes otro PostgreSQL en :5432)
docker compose -f docker-compose.db.yml up -d

# Opción alternativa: contenedor manual
docker run --name credit-postgres \
  -e POSTGRES_DB=credit_evaluation_db \
  -e POSTGRES_USER=austro_user \
  -e POSTGRES_PASSWORD=austro_pass \
  -p 5433:5432 -d postgres:16
```

### Microservicio de Riesgos (terminal 1)
```bash
cd ms-risk-mock-credit-evaluation
./mvnw quarkus:dev
```

### Microservicio Orquestador (terminal 2)
```bash
cd ms-orchestrator-credit-evaluation
./mvnw quarkus:dev
```

### Frontend Angular (terminal 3)
```bash
cd mms-ux-credit-evaluation
npm install
npm start
```

---

## Ejecutar Tests

### Orquestador — Tests unitarios y de integración
```bash
cd ms-orchestrator-credit-evaluation
./mvnw test
```

### Mock de Riesgos — Tests de integración
```bash
cd ms-risk-mock-credit-evaluation
./mvnw test
```

---

## Flujo de una Evaluación

```
Usuario (Angular Form)
  │ POST /v1/credit-evaluations
  ▼
ms-orchestrator (puerto 8080)
  ├── Valida cédula (Módulo 10 Ecuador)
  ├── GET /v1/risk-score/{cedula}  ──────► ms-risk-mock (latencia 2s)
  ├── GET /v1/customer-debts/{cedula} ───► ms-risk-mock (latencia 1.5s)
  ├── Aplica regla de negocio:
  │     score > 70 AND (deudas + cuota) < (salario * 0.40)
  └── Persiste en PostgreSQL
        │ HTTP 201 {finalStatus, id, ...}
        ▼
Usuario ve: ✅ APROBADO | ❌ RECHAZADO
```

---

## Regla de Negocio

```
APROBADO si:
  score > 70
  AND
  (Σ deudas_mensuales + (monto_solicitado / (años × 12))) < (salario × 0.40)

RECHAZADO en cualquier otro caso.
```

---

## Stack Tecnológico

| Componente | Tecnología | Versión |
|---|---|---|
| Frontend | Angular + TypeScript | 21.2 / TS 5.9 |
| Microservicio A | Quarkus + Java | 3.8.4 / Java 21 |
| Microservicio B | Quarkus + Java | 3.8.4 / Java 21 |
| Base de Datos | PostgreSQL | 16 |
| Comunicación intra-servicio | MicroProfile REST Client | Quarkus built-in |
| Migraciones BD | Flyway | Quarkus built-in |
| ORM | Hibernate ORM + Panache | Quarkus built-in |
| Testing BE | JUnit 5 + RestAssured + WireMock | — |
| Contenedores | Docker + Docker Compose | 24+ |

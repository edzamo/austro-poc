# ms-orchestrator-credit-evaluation

Microservicio orquestador de evaluaciones de crédito construido con **Quarkus 3.8** y **Java 21**, siguiendo **Arquitectura Hexagonal (Puertos y Adaptadores)**.

---

## Decisión de Arquitectura: REST Client vs gRPC

### Elección: MicroProfile REST Client (Quarkus REST Client Reactive)

**Justificación:**

| Factor | REST Client | gRPC |
|---|---|---|
| **Complejidad de setup** | Mínima — interfaz Java anotada | Alta — requiere `.proto`, `protoc`, código generado |
| **Contrato de API** | OpenAPI/Swagger (ya incluido) | Protocol Buffers (binario, tipado estrictamente) |
| **Rendimiento** | HTTP/1.1 JSON — suficiente para latencias de 1.5-2s | HTTP/2 binario — ventaja real en streaming o alta frecuencia |
| **Resiliencia** | `@Retry`, `@Timeout`, `@CircuitBreaker` vía MicroProfile | Interceptores gRPC — más verboso de configurar |
| **Debugabilidad** | Curl, Postman, logs legibles | Requiere herramientas especializadas (grpcurl, Bloom RPC) |
| **Idoneidad para PoC** | ✅ Ideal | ⚠️ Overkill |

**Conclusión:** Para este ecosistema donde el servicio de riesgo simula latencias de 1.5 a 2 segundos por llamada, la overhead de serialización JSON vs Protocol Buffers es insignificante. REST ofrece máxima compatibilidad, contratos claros vía OpenAPI, y menor complejidad de onboarding. gRPC sería la elección correcta si se necesitara **streaming bidireccional**, comunicación en microsegundos, o el servicio fuera consumido por múltiples tecnologías heterogéneas.

---

## Arquitectura Hexagonal (Puertos y Adaptadores)

```
ms-orchestrator-credit-evaluation/
└── src/main/java/com/austro/orchestrator/
    │
    ├── domain/                        ← Núcleo puro: SOLO entidades y servicios de dominio
    │   ├── model/
    │   │   ├── CreditEvaluation.java  ← Entidad de dominio (sin dependencias de framework)
    │   │   ├── EvaluationStatus.java  ← Enum: APROBADO | RECHAZADO
    │   │   └── RiskData.java          ← Value Object con score y deudas
    │   └── service/                   ← Lógica de negocio pura (sin I/O, sin CDI, sin JPA)
    │       ├── CedulaValidatorService.java   ← Algoritmo Módulo 10 (regla de negocio pura)
    │       └── CreditApprovalService.java    ← Regla de aprobación (score + ratio de deuda)
    │
    ├── application/                   ← Frontera del hexágono: puertos + casos de uso
    │   ├── port/
    │   │   ├── in/                    ← Puertos de ENTRADA: lo que la app expone al exterior
    │   │   │   ├── EvaluateCreditUseCase.java
    │   │   │   └── GetEvaluationsUseCase.java
    │   │   └── out/                   ← Puertos de SALIDA: lo que la app necesita del exterior
    │   │       ├── CreditEvaluationRepository.java
    │   │       └── RiskServicePort.java
    │   └── usecase/                   ← Implementaciones de los casos de uso
    │       ├── EvaluateCreditUseCaseImpl.java
    │       └── GetEvaluationsUseCaseImpl.java
    │
    └── infrastructure/                ← Adaptadores: conectan los puertos al mundo real
        ├── rest/                      ← Adaptador de ENTRADA: implementa el protocolo HTTP
        │   ├── CreditEvaluationResource.java  ← llama puertos IN
        │   ├── dto/
        │   └── exception/
        ├── persistence/               ← Adaptador de SALIDA: implementa CreditEvaluationRepository
        │   ├── entity/CreditEvaluationEntity.java
        │   └── repository/CreditEvaluationRepositoryImpl.java
        └── client/                    ← Adaptador de SALIDA: implementa RiskServicePort
            ├── RiskServiceClient.java
            └── RiskServiceAdapter.java
```

**Por qué los puertos están en `application` y no en `domain`:**

| Artefacto | Capa | Razón |
|---|---|---|
| `CreditEvaluation`, `RiskData` | `domain/model` | Son conceptos del negocio, sin dependencias externas |
| `CedulaValidatorService` | `domain/service` | Algoritmo puro (Módulo 10), no habla con nadie |
| `CreditApprovalService` | `domain/service` | Regla de aprobación pura, sin I/O |
| `EvaluateCreditUseCase` (port in) | `application/port/in` | Define la frontera de la *aplicación*, no del dominio |
| `CreditEvaluationRepository` (port out) | `application/port/out` | Contrato de orquestación que el caso de uso necesita |
| `EvaluateCreditUseCaseImpl` | `application/usecase` | Orquesta el dominio usando los puertos |
| Adaptadores REST/JPA/REST-Client | `infrastructure` | Implementan los puertos con tecnología concreta |

Los puertos pertenecen a `application` porque definen los límites del hexágono — son la forma en que la aplicación se comunica hacia adentro (puertos IN) y hacia afuera (puertos OUT). El dominio puro solo contiene reglas de negocio que no dependen de ningún I/O.

---

## Regla de Negocio de Aprobación

```
APROBADO si:
  score > 70
  AND
  (Σ deudas_mensuales + (monto_solicitado / (años * 12))) < (salario * 0.40)
```

---

## Configuración y Ejecución

### Requisitos
- Java 21+
- Maven 3.9+
- Docker (para PostgreSQL)

### Levantar la base de datos
```bash
docker run --name credit-db -e POSTGRES_DB=credit_evaluation_db \
  -e POSTGRES_USER=austro_user -e POSTGRES_PASSWORD=austro_pass \
  -p 5432:5432 -d postgres:16
```

### Ejecutar en modo desarrollo
```bash
./mvnw quarkus:dev
```

### Ejecutar tests
```bash
./mvnw test
```

### Variables de entorno
| Variable | Valor por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/credit_evaluation_db` | URL de la base de datos |
| `DB_USERNAME` | `austro_user` | Usuario de la BD |
| `DB_PASSWORD` | `austro_pass` | Contraseña de la BD |
| `RISK_SERVICE_URL` | `http://localhost:8081` | URL del ms-risk-mock |

### Endpoints disponibles
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/v1/credit-evaluations` | Evalúa una solicitud de crédito |
| `GET` | `/v1/credit-evaluations` | Lista el historial de evaluaciones |
| `GET` | `/q/health` | Health check |
| `GET` | `/swagger-ui` | Documentación interactiva |

# mms-ux-credit-evaluation

Frontend Angular 18 para el ecosistema de evaluación de créditos, implementando **Clean Architecture** con componentes standalone.

---

## Justificación: Clean Architecture en Angular

La Clean Architecture separa el código en capas con dependencias unidireccionales. Las capas internas no conocen a las externas.

```
mms-ux-credit-evaluation/src/app/
│
├── core/                          ← NÚCLEO (sin dependencias de Angular/HTTP)
│   ├── domain/
│   │   ├── models/                ← Entidades de negocio puras (interfaces TypeScript)
│   │   │   └── credit-evaluation.model.ts
│   │   └── ports/                 ← Contratos de salida (clases abstractas)
│   │       └── credit-evaluation.port.ts
│   │
│   └── application/               ← Casos de uso (usan el puerto, no la implementación)
│       └── use-cases/
│           ├── submit-evaluation.use-case.ts
│           └── get-evaluations.use-case.ts
│
├── infrastructure/                ← ADAPTADORES (implementan los puertos con tecnología)
│   └── services/
│       └── credit-evaluation-api.service.ts  ← Usa HttpClient, conoce la URL
│
└── presentation/                  ← UI (usa los casos de uso, no la infraestructura)
    ├── credit-evaluation-form/    ← Formulario con manejo de estados
    └── evaluation-list/           ← Tabla de historial
```

### ¿Por qué este patrón aquí?

| Beneficio | Cómo se logra |
|---|---|
| **Testabilidad** | Los casos de uso se testean con un mock del puerto, sin HTTP |
| **Flexibilidad** | Cambiar el backend (REST → GraphQL) solo afecta `infrastructure/` |
| **Separación de concerns** | La UI no sabe nada de HTTP; la infraestructura no sabe nada de formularios |
| **Tipado fuerte** | Los modelos de dominio son la única fuente de verdad del tipo |

### Flujo de datos

```
Template (HTML)
    ↓ eventos
Component (Presentation)
    ↓ llama a
Use Case (Application)
    ↓ usa el puerto (abstracción)
CreditEvaluationPort ← implementado por → CreditEvaluationApiService (Infrastructure)
    ↓ HTTP
Backend API (ms-orchestrator-credit-evaluation)
```

---

## Características de la UI

- **Formulario reactivo** con validaciones: cédula (10 dígitos), montos positivos, años válidos.
- **Estados de carga**: botón deshabilitado + spinner mientras se procesa.
- **Resultado visual**: badge verde APROBADO / rojo RECHAZADO con datos del registro persistido.
- **Manejo de errores**: muestra el mensaje del backend o un mensaje genérico.
- **Historial**: tabla paginable con todas las evaluaciones cargadas al inicio.
- **Responsive**: diseño adaptable a móvil.
- **Standalone components**: no usa NgModules, siguiendo las mejores prácticas de Angular 18.

---

## Configuración y Ejecución

### Requisitos
- Node.js 20+ LTS
- npm 10+

### Instalar dependencias
```bash
npm install
```

### Ejecutar en desarrollo
```bash
npm start
# Disponible en: http://localhost:4200
```

### Construir para producción
```bash
npm run build
```

### Variables de entorno
Editar `src/environments/environment.ts`:
```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080'  // URL del ms-orchestrator
};
```

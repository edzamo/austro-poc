# Seguridad — Evaluación de Créditos PoC

Cobertura de los dos requisitos de seguridad del sistema: protección contra inyección SQL y CORS.

---

## 1. Protección contra Inyección SQL

### Por qué el sistema es seguro

Ningún dato del usuario llega directamente a una query SQL. El flujo completo es:

```
Request HTTP → Jackson (deserialización tipada) → Bean Validation → Lógica de negocio
                                                                          │
                                                              Hibernate ORM / Panache
                                                                          │
                                                              Prepared Statements
                                                                          │
                                                                    PostgreSQL
```

**Dos capas de protección:**

| Capa | Mecanismo | Qué bloquea |
|---|---|---|
| Deserialización | Jackson mapea JSON a tipos Java (`BigDecimal`, `Integer`) | Texto en campos numéricos → rechazado antes del use case |
| Validación de cédula | `cedula.matches("\\d+")` — solo dígitos | Cualquier carácter especial (`;`, `'`, `--`) en el campo cédula |
| ORM | Panache `persist()` y `listAll()` usan prepared statements | Imposible concatenar input de usuario en la query |

**Lo que Hibernate genera internamente:**

```sql
-- Tú llamas: persist(entity)
-- Hibernate envía a PostgreSQL:
INSERT INTO credit_evaluations (cedula, requested_amount, years, salary, final_status, evaluation_date)
VALUES ($1, $2, $3, $4, $5, $6)
-- Los valores viajan como parámetros tipados, NUNCA como texto en la query
```

No existe en todo el proyecto ningún `nativeQuery`, `createQuery("..." + variable)` ni concatenación de input de usuario en SQL.

---

### Pruebas de inyección SQL

> **Prerrequisito:** tener el orquestador corriendo en `http://localhost:8080`
> ```bash
> cd ms-orchestrator-credit-evaluation && ./mvnw quarkus:dev
> ```

---

#### Prueba 1 — Inyección en el campo `cedula`

```bash
curl -X POST http://localhost:8080/v1/credit-evaluations \
  -H "Content-Type: application/json" \
  -d '{
    "cedula": "1713175071; DROP TABLE credit_evaluations; --",
    "requestedAmount": 5000,
    "years": 2,
    "salary": 1500
  }'
```

**Respuesta esperada:**
```json
HTTP/1.1 422 Unprocessable Entity
{
  "status": 422,
  "error": "Cédula Inválida",
  "message": "La cédula '1713175071; DROP TABLE credit_evaluations; --' no es válida según el algoritmo Módulo 10 de Ecuador.",
  "timestamp": "2026-08-05T20:00:00"
}
```

La cédula contiene caracteres no numéricos → `CedulaValidatorService.isValid()` retorna `false` → se lanza `CedulaInvalidaException` → HTTP 422. La base de datos **nunca es contactada**.

---

#### Prueba 2 — Inyección en campo numérico (`requestedAmount`)

```bash
curl -X POST http://localhost:8080/v1/credit-evaluations \
  -H "Content-Type: application/json" \
  -d '{
    "cedula": "1713175071",
    "requestedAmount": "1 OR 1=1",
    "years": 2,
    "salary": 1500
  }'
```

**Respuesta esperada:**
```json
HTTP/1.1 400 Bad Request
```

Jackson no puede deserializar `"1 OR 1=1"` al tipo `BigDecimal` → el request falla en la capa HTTP antes de llegar al use case.

---

#### Prueba 3 — Inyección con comillas en `cedula`

```bash
curl -X POST http://localhost:8080/v1/credit-evaluations \
  -H "Content-Type: application/json" \
  -d '{
    "cedula": "'' OR ''1''=''1",
    "requestedAmount": 5000,
    "years": 2,
    "salary": 1500
  }'
```

**Respuesta esperada:**
```json
HTTP/1.1 422 Unprocessable Entity
{
  "status": 422,
  "error": "Cédula Inválida",
  "message": "..."
}
```

Comillas no son dígitos → rechazado por `matches("\\d+")` antes de tocar la BD.

---

#### Prueba 4 — Verificar que la tabla sobrevive a todos los intentos

Después de ejecutar las pruebas anteriores, conecta directamente a PostgreSQL:

```bash
docker exec -it credit-postgres psql -U austro_user -d credit_evaluation_db \
  -c "SELECT COUNT(*) FROM credit_evaluations;"
```

**Resultado esperado:**
```
 count
-------
     0
(1 row)
```

La tabla existe y no fue alterada. Si devuelve un número (aunque sea 0), el `DROP TABLE` no tuvo ningún efecto.

---

#### Prueba 5 — Solicitud legítima (control positivo)

Confirma que una cédula válida sí pasa correctamente:

```bash
curl -X POST http://localhost:8080/v1/credit-evaluations \
  -H "Content-Type: application/json" \
  -d '{
    "cedula": "1713175071",
    "requestedAmount": 10000,
    "years": 3,
    "salary": 2000
  }'
```

**Respuesta esperada:**
```json
HTTP/1.1 201 Created
{
  "id": 1,
  "cedula": "1713175071",
  "requestedAmount": 10000.00,
  "years": 3,
  "salary": 2000.00,
  "finalStatus": "APROBADO",
  "evaluationDate": "2026-08-05T20:00:00"
}
```

---

## 2. CORS — Cross-Origin Resource Sharing

### Configuración aplicada

```properties
# ms-orchestrator-credit-evaluation/src/main/resources/application.properties

quarkus.http.cors=true
quarkus.http.cors.origins=http://localhost:4200        # Solo el frontend Angular
quarkus.http.cors.methods=GET,POST,OPTIONS             # Solo métodos necesarios
quarkus.http.cors.headers=accept,authorization,content-type,x-requested-with
```

El backend solo responde con `Access-Control-Allow-Origin` al origen `http://localhost:4200`. Cualquier otro origen (otro puerto, otro dominio) recibe una respuesta sin ese header → el navegador bloquea la respuesta.

---

### Pruebas de CORS

> **Prerrequisito:** orquestador corriendo en `http://localhost:8080`

---

#### Prueba 1 — Origen no autorizado (debe ser bloqueado)

```bash
curl -i \
  -H "Origin: http://evil.com" \
  -H "Access-Control-Request-Method: POST" \
  -X OPTIONS http://localhost:8080/v1/credit-evaluations
```

**Respuesta esperada:** sin `Access-Control-Allow-Origin` en los headers:
```
HTTP/1.1 200 OK
content-length: 0
# No aparece: Access-Control-Allow-Origin
```

El navegador recibe esta respuesta sin el header CORS → bloquea la llamada con error de política CORS. Un script en `evil.com` no puede leer la respuesta.

---

#### Prueba 2 — Origen autorizado (debe pasar)

```bash
curl -i \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type" \
  -X OPTIONS http://localhost:8080/v1/credit-evaluations
```

**Respuesta esperada:**
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Methods: GET,POST,OPTIONS
Access-Control-Allow-Headers: accept,authorization,content-type,x-requested-with
```

El frontend Angular en `localhost:4200` recibe permiso explícito.

---

#### Prueba 3 — Método no permitido desde origen autorizado

```bash
curl -i \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: DELETE" \
  -X OPTIONS http://localhost:8080/v1/credit-evaluations
```

**Respuesta esperada:** `DELETE` no aparece en `Access-Control-Allow-Methods`:
```
HTTP/1.1 200 OK
Access-Control-Allow-Methods: GET,POST,OPTIONS
# DELETE no está incluido → el navegador bloquea la llamada
```

---

### Configuración para producción

Antes de desplegar en producción, actualizar el origen en `application.properties`:

```properties
# Reemplazar localhost por el dominio real del frontend
quarkus.http.cors.origins=https://creditos.austro.com
```

O usando variable de entorno en `docker-compose.yml`:

```yaml
environment:
  QUARKUS_HTTP_CORS_ORIGINS: https://creditos.austro.com
```

---

## Resumen de cobertura

| Requisito | Estado | Evidencia |
|---|---|---|
| Protección SQL Injection | ✅ Cubierto | Hibernate ORM prepared statements + validación previa en `CedulaValidatorService` |
| Validación de tipos | ✅ Cubierto | Jackson rechaza texto en campos numéricos antes del use case |
| CORS seguro | ✅ Cubierto | Origen restringido a `localhost:4200`, métodos y headers explícitos |
| CORS producción | ⚠️ Pendiente | Cambiar `quarkus.http.cors.origins` al dominio real antes de desplegar |

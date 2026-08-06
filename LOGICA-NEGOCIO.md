# Lógica de Negocio — Evaluación de Créditos

---

## 1. Qué hace el sistema

El sistema recibe una solicitud de crédito con los datos del cliente y devuelve automáticamente si está **APROBADO** o **RECHAZADO**. Para tomar esa decisión consulta un servicio externo de riesgo (bureau de crédito simulado) y aplica una regla financiera interna.

---

## 2. Datos que el cliente envía

| Campo | Tipo | Ejemplo | Validaciones |
|---|---|---|---|
| `cedula` | String 10 dígitos | `1712345678` | Algoritmo Módulo 10 Ecuador |
| `requestedAmount` | Decimal | `15000.00` | Positivo, obligatorio |
| `years` | Entero | `3` | Positivo, obligatorio |
| `salary` | Decimal | `2000.00` | Positivo, obligatorio |

---

## 3. Validación de la cédula (Módulo 10 Ecuador)

Antes de consultar cualquier servicio, el sistema valida que la cédula sea ecuatoriana legítima. Se rechazan las solicitudes con cédula inválida con `HTTP 422`.

**Reglas en orden de verificación:**

```
1. Exactamente 10 dígitos numéricos (sin letras, sin espacios)

2. Los dos primeros dígitos = código de provincia
      Válidos: 01 al 24  (provincias continentales e insulares)
      Válido:  30         (personas jurídicas)
      Inválido: 00, 25-29, 31+

3. El tercer dígito debe ser < 6
      0-5 → persona natural ✅
      6-9 → entidad jurídica / especial ❌ (fuera de scope)

4. Algoritmo Módulo 10 sobre los primeros 9 dígitos:
      - Posiciones pares (índice 0,2,4,6,8) × 2  →  si resultado > 9, restar 9
      - Posiciones impares (índice 1,3,5,7) × 1
      - Sumar todos → suma
      - Dígito esperado = (10 - (suma % 10)) % 10
      - Debe coincidir con el décimo dígito (dígito verificador)
```

**Ejemplo con cédula `1712345678`:**

```
Dígitos:  1  7  1  2  3  4  5  6  7  8
Índice:   0  1  2  3  4  5  6  7  8  9
Coef:     2  1  2  1  2  1  2  1  2  —

1×2=2  7×1=7  1×2=2  2×1=2  3×2=6  4×1=4  5×2=10→1  6×1=6  7×2=14→5
Suma = 2+7+2+2+6+4+1+6+5 = 35
Dígito esperado = (10 - (35 % 10)) % 10 = (10 - 5) % 10 = 5
Dígito verificador (posición 9) = 8  →  NO coincide  →  cédula inválida
```

---

## 4. Mock del Servicio de Riesgo (ms-risk-mock-credit-evaluation)

Simula un bureau de crédito externo con dos endpoints independientes. Cada llamada genera datos **aleatorios** — la misma cédula puede dar resultados distintos en consultas sucesivas. Esto es intencional para simular la variación real de un bureau.

---

### GET `/v1/risk-score/{cedula}` — Score de riesgo

**Qué simula:** la consulta al historial crediticio del cliente en el bureau.

**Lógica interna:**

```
1. Espera 2.000 ms  (simula latencia de red + procesamiento del bureau)
2. Genera un entero aleatorio entre 0 y 100 inclusive
      score = Random.nextInt(101)   →  rango [0, 100]
3. Retorna el score junto con la cédula recibida
```

**Ejemplo de respuesta:**
```json
HTTP 200 OK
{
  "cedula": "0102030405",
  "score": 83
}
```

**Interpretación del score en el orquestador:**

| Rango | Significado |
|---|---|
| 0 – 70 | Perfil de riesgo alto → RECHAZADO directo |
| 71 – 100 | Perfil aceptable → continúa con evaluación de capacidad de pago |

> El umbral es estricto: `score > 70`, por lo tanto score = 70 es RECHAZADO.

---

### GET `/v1/customer-debts/{cedula}` — Deudas del cliente

**Qué simula:** la consulta a las obligaciones financieras vigentes del cliente en el sistema financiero.

**Lógica interna:**

```
1. Espera 1.500 ms  (simula latencia del bureau)
2. Decide cuántas deudas tiene el cliente:
      numberOfDebts = Random.nextInt(4)   →  valores posibles: 0, 1, 2 o 3
3. Mezcla aleatoriamente el catálogo de tipos de deuda:
      Catálogo fijo:
        - "Tarjeta de Crédito"
        - "Préstamo Hipotecario"
        - "Préstamo Personal"
        - "Crédito Automotriz"
        - "Línea de Crédito"
4. Por cada deuda genera una cuota mensual aleatoria:
      monthlyPayment = 50 + Random.nextInt(951)   →  rango [$50.00, $1.000.00]
      (redondeado a 2 decimales con HALF_UP)
5. Retorna la lista de deudas (puede ser vacía si numberOfDebts = 0)
```

**Ejemplo de respuesta con 2 deudas:**
```json
HTTP 200 OK
{
  "debts": [
    { "debtName": "Tarjeta de Crédito",  "monthlyPayment": 215.00 },
    { "debtName": "Crédito Automotriz",  "monthlyPayment": 480.50 }
  ]
}
```

**Ejemplo de respuesta sin deudas:**
```json
HTTP 200 OK
{
  "debts": []
}
```

**Impacto en la evaluación:**

| Situación | Efecto |
|---|---|
| Lista vacía | El cliente no tiene compromisos previos — solo se suma la cuota nueva |
| 1-3 deudas | Se suman todas las cuotas mensuales al cálculo de capacidad de pago |
| Deuda alta (ej. $900) | Puede hacer que incluso un monto pequeño supere el límite del 40% |

---

## 5. Flujo de llamadas: paralelismo

El orquestador llama a los dos endpoints del mock **en paralelo**, no de forma secuencial:

```
Orquestador
   ├──► GET /v1/risk-score/{cedula}      ⏱ ~2.0s  ─┐
   └──► GET /v1/customer-debts/{cedula}  ⏱ ~1.5s  ─┴─► ambas resueltas en ~2s
```

Si fueran secuenciales tardaría ~3.5s. Con paralelismo el tiempo total es el de la llamada más lenta (~2s).

---

## 6. Regla de aprobación crediticia

Con el score y las deudas del mock, el orquestador aplica dos condiciones en orden. **Ambas deben cumplirse** para aprobar.

### Condición 1 — Score mínimo

```
score > 70   →  continúa a condición 2
score ≤ 70   →  RECHAZADO inmediatamente (no se evalúa capacidad de pago)
```

### Condición 2 — Capacidad de pago (ratio deuda/ingreso máximo 40%)

```
cuota_nueva        = monto_solicitado / (años × 12)

total_compromisos  = Σ(deudas_mensuales_del_mock) + cuota_nueva

límite_permitido   = salario × 0.40

total_compromisos < límite_permitido  →  APROBADO
total_compromisos ≥ límite_permitido  →  RECHAZADO
```

---

## 7. Ejemplos completos

### Caso APROBADO

```
Entrada:        cédula válida, salario $2.000, monto $12.000 a 3 años
Mock score:     85              → pasa condición 1 ✅
Mock deudas:    Tarjeta $200
Cuota nueva:    $12.000 / 36 = $333.33
Total:          $200 + $333.33 = $533.33
Límite:         $2.000 × 0.40 = $800.00
$533.33 < $800.00              → pasa condición 2 ✅

Resultado: APROBADO
```

### Caso RECHAZADO por score bajo

```
Entrada:        cédula válida, salario $3.000, monto $5.000 a 1 año
Mock score:     65              → falla condición 1 ❌

Resultado: RECHAZADO  (no se evalúa capacidad de pago)
```

### Caso RECHAZADO por capacidad de pago

```
Entrada:        cédula válida, salario $1.000, monto $10.000 a 2 años
Mock score:     90              → pasa condición 1 ✅
Mock deudas:    Préstamo Personal $250
Cuota nueva:    $10.000 / 24 = $416.67
Total:          $250 + $416.67 = $666.67
Límite:         $1.000 × 0.40 = $400.00
$666.67 ≥ $400.00              → falla condición 2 ❌

Resultado: RECHAZADO
```

### Caso RECHAZADO con lista de deudas vacía (solo por score)

```
Entrada:        cédula válida, salario $5.000, monto $1.000 a 1 año
Mock score:     40              → falla condición 1 ❌
Mock deudas:    [] (sin deudas)

Resultado: RECHAZADO  (score bajo pese a no tener ninguna deuda)
```

---

## 8. Lo que se guarda en la base de datos

Cada evaluación procesada — aprobada o rechazada — queda registrada en la tabla `credit_evaluations`.

### Tabla `credit_evaluations`

| Columna | Tipo PostgreSQL | Descripción |
|---|---|---|
| `id` | `BIGSERIAL` | Identificador autoincremental, clave primaria |
| `cedula` | `VARCHAR(10)` | Cédula del solicitante, ya validada |
| `requested_amount` | `NUMERIC(15,2)` | Monto solicitado en USD |
| `years` | `INTEGER` | Plazo del crédito en años |
| `salary` | `NUMERIC(15,2)` | Salario mensual declarado |
| `final_status` | `VARCHAR(10)` | `APROBADO` o `RECHAZADO` |
| `evaluation_date` | `TIMESTAMP` | Fecha y hora UTC del procesamiento |

**Constraints:**
- `final_status CHECK` solo acepta `'APROBADO'` o `'RECHAZADO'`
- Todos los campos son `NOT NULL`

**Índices:**
- `idx_credit_evaluations_cedula` — búsqueda por cédula
- `idx_credit_evaluations_date DESC` — historial ordenado por más reciente

### Ejemplo de fila guardada

```sql
SELECT * FROM credit_evaluations WHERE id = 1;

 id | cedula     | requested_amount | years | salary  | final_status | evaluation_date
----+------------+------------------+-------+---------+--------------+---------------------
  1 | 0102030405 |        12000.00  |     3 | 2000.00 | APROBADO     | 2026-08-05 14:23:11
```

### Lo que NO se guarda

El score y el detalle de las deudas **no se persisten**. Vienen del mock en tiempo real, se usan para calcular el resultado y se descartan. El registro almacena únicamente los datos de la solicitud y la decisión final.

---

## 9. Respuesta HTTP al cliente

```json
HTTP 201 Created
{
  "id": 1,
  "cedula": "0102030405",
  "requestedAmount": 12000.00,
  "years": 3,
  "salary": 2000.00,
  "finalStatus": "APROBADO",
  "evaluationDate": "2026-08-05T14:23:11"
}
```

**Códigos HTTP posibles:**

| Código | Situación |
|---|---|
| `201 Created` | Evaluación procesada correctamente (APROBADO o RECHAZADO) |
| `422 Unprocessable Entity` | Cédula inválida o campos de validación fallidos |
| `400 Bad Request` | JSON malformado o campos faltantes |
| `503 Service Unavailable` | Error al consultar el servicio de riesgo mock |

# CLAUDE.md — OmniStack Microservice
## Contexto completo para continuar el desarrollo con Claude Code

---

## 1. ¿Qué es este proyecto?

**OmniStack** es un microservicio unificado que expone una API REST JSON al front-end (POS / Caja de farmacia) y traduce internamente las peticiones hacia 5 proveedores externos con protocolos y contratos distintos.

El front **NUNCA** llama directamente a los proveedores. Solo conoce el contrato OmniStack.

```
FRONT (POS)  ──►  OmniStack API  ──►  ECUABET       (REST/JSON)
                                  ──►  LN BET593      (REST/JSON)
                                  ──►  LN PEGA3       (REST/JSON)
                                  ──►  LN TRADICIONALES (REST/JSON)
                                  ──►  CLARO          (SOAP/XML)
```

---

## 2. Archivos de referencia — leerlos antes de generar cualquier código

| Archivo | Contenido | Prioridad |
|---|---|---|
| `MapeoCampos_v8.xlsx` | Mapeo campo OmniStack → campo proveedor para cada fase y cada proveedor | ⭐⭐⭐ CRÍTICO |
| `Documento_Tecnico_EndPoints_V02_Actualizado.docx` | Spec completa de todos los endpoints OmniStack | ⭐⭐⭐ CRÍTICO |
| `OmniStack_postman_collection_v8.json` | Contratos JSON de request/response con ejemplos reales | ⭐⭐⭐ CRÍTICO |
| `OmniStack-TST_postman_environment_v3.json` | Variables de entorno: URLs, credenciales, parámetros por proveedor | ⭐⭐ IMPORTANTE |
| `OmniStack_Diagramas_Azul.drawio` | Diagramas de secuencia de los 7 flujos | ⭐⭐ IMPORTANTE |
| `BET593_*.json` / `03_*.json` ... `17_*.json` | Mocks individuales de request/response por fase y proveedor | ⭐ REFERENCIA |

---

## 3. Endpoints OmniStack — Contrato que debe exponer el microservicio

### Rutas definidas

```
POST /business-lines          → Catálogo de servicios disponibles por POS
POST /v1/preCheck             → Validación previa (consulta al proveedor)
POST /v1/createTicket         → Crear ticket Pega3 (EXCLUSIVO LN Pega3)
POST /v1/execute              → Ejecutar la transacción
POST /v1/verify               → Verificar resultado post-execute
POST /v1/reverse              → Reversar / anular transacción
POST /v1/conciliate           → Conciliación (PENDIENTE — sin spec aún)
```

### Campos de contexto obligatorios en TODOS los endpoints (request y response)

```json
{
  "uuid":                   "string(200)  — ID único de transacción por flujo",
  "chain":                  "string(10)   — Código de cadena",
  "store":                  "string(10)   — Código del local",
  "store_name":             "string(200)  — Nombre del local",
  "pos":                    "string(10)   — Número de caja/POS",
  "channel_POS":            "string(3)    — POS | WEB | APP | CCT",
  "category_code":          "integer(11)  — Código de categoría",
  "subcategory_code":       "integer(11)  — Código de subcategoría",
  "service_provider_code":  "integer(11)  — Código del proveedor",
  "rms_item_code":          "string(100)  — SKU del servicio"
}
```

### Estructura de response estándar (todos los endpoints excepto /business-lines)

```json
{
  "...campos de contexto...",
  "is_error": "boolean — SIEMPRE presente",
  "error": {
    "code":    "string(200)",
    "message": "string(200)"
  },
  "status": {
    "code":    "string(200) — 00=OK | 01=Error | 02=Usuario inválido",
    "message": "string(200)"
  },
  "authorization": "string(200) — ID seguimiento externo del proveedor"
}
```

---

## 4. Proveedores — Datos críticos de integración

### 4.1 ECUABET
- **Protocolo:** REST JSON
- **URL DEV:** `https://apidev.virtualsoft.tech/operatorapi-new/`
- **Auth:** Header con `shop` (ID) + `token` (sesión) — INTERNOS, no vienen del front
- **Flujo CASH_IN:** `PRECHECK(BuscarUsuario)` → `EXECUTE(Deposito)` → `[REVERSE(Rollback)]`
- **Flujo CASH_OUT:** `PRECHECK(ConsultaNotaRetiro)` → `EXECUTE(NotaRetiro)` → `[REVERSE(Rollback)]`
- **Mapeo clave:**
  - `authorization` ← `transactionId`
  - `userid` → campo `userid` Ecuabet (obligatorio en EXECUTE)

### 4.2 LN BET593
- **Protocolo:** REST JSON
- **URL DEV:** `https://www8.loteria.com.ec/APIVentasLoteria/`
- **Auth:** LOGIN interno (POST `/api/Ventas/Login`) → devuelve `token`. OmniStack gestiona la sesión.
- **Flujo CASH_IN:** `LOGIN` → `PRECHECK(RecargarBet593)` → `EXECUTE(ConfirmarBet593)` → `VERIFY(ValidarBet593)` → `[REVERSE(ReversarBet593)]`
- **Flujo CASH_OUT:** `LOGIN` → `PRECHECK(ConsultarRetiroBet593)` → `EXECUTE(RetirarBet593)` → `VERIFY(ConsultarRetiro)` → `[REVERSE(ReversarRetiroBet593)]`
- **CRÍTICO — campos que deben persistirse en sesión:**
  - CASH_IN: `recargaid` + `serialnumber` (del PRECHECK response) → OBLIGATORIOS en EXECUTE/VERIFY/REVERSE
  - CASH_OUT: `ordenPagoId` (del EXECUTE response) → OBLIGATORIO en VERIFY/REVERSE
- **CRÍTICO — lógica VERIFY:**
  - Si `estado = COMMIT` → transacción OK ✅
  - Si `estado = ROLLBACK` → OmniStack debe disparar REVERSE automáticamente ❌

### 4.3 LN PEGA3
- **Protocolo:** REST JSON
- **URL DEV:** `https://www8.loteria.com.ec/APIVentasLoteria/`
- **Auth:** LOGIN interno → `token` + `deviceId`
- **Flujo CASH_IN:** `LOGIN` → `PRECHECK(VentaProductos + ObtieneSorteosActivo)` → `CREATE_TICKET(CrearTicket)` → `EXECUTE(confirmar cobro)` → `VERIFY(GenerarComprobantePega)` → `[REVERSE(CancelarTicket)]`
- **CASH_IN PRECHECK hace 2 llamadas al proveedor:**
  1. `POST /api/Ventas/VentaProductos` → config del juego (montos, jugadas, tipos de entrada)
  2. `POST /api/Ventas/ObtieneSorteosActivo` → sorteo activo (drawNumber, drawDate)
- **CRÍTICO:** `gameTicketNumber` del CREATE_TICKET response debe persistirse para EXECUTE/VERIFY/REVERSE
- **REVERSO tiene ventana de 5 minutos** post-venta. Pasado ese tiempo, falla.
- **VERIFY llama a:** `GET /api/Ventas/GenerarComprobantePega?ventaId=&idUsuario=&transaccion=&puntoDeVenta=`
- **Productos Pega:** `code=1001 (Pega3) | 1002 (Pega4) | 1004 (Pega2)` — determinar por rms_item_code

### 4.4 LN TRADICIONALES
- **Protocolo:** REST JSON
- **URL DEV:** `https://www8.loteria.com.ec/APIVentasLoteria/`
- **Auth:** LOGIN interno (diferente endpoint — Tradicionales usa `/api/Ventas/Login` con respuesta `{codError, token}`)
- **Flujo CASH_IN:** `LOGIN` → `PRECHECK(4 llamadas)` → `EXECUTE(VentaBoletos)` → `VERIFY(GenerarComprobanteVenta)` → `[REVERSE(AnularVentaBoletos)]`
- **PRECHECK hace 4 llamadas al proveedor en secuencia:**
  1. `RecuperarJuegosPorMedio` → lista de juegos
  2. `RecuperarSorteosDisponibles` → sorteos por juego
  3. `RecuperarFigurasPorJuego` ← solo Lotería (opcional)
  4. `RecuperarNumerosDisponiblesPorCombinacion` → números disponibles
- **CRÍTICO:** `ventaId` del EXECUTE response → persistir para VERIFY/REVERSE
- **Juegos:** `juegoId=1 (Lotería) | 2 (Lotto) | 5 (Pozo Millonario)`

### 4.5 CLARO
- **⚠️ IMPORTANTE:** NO es REST. Es **SOAP/XML document-literal**
- **URL DEV:** `http://192.168.37.40:50004/sprXslt/SprXsltWSService`
- **URL PRODUCCIÓN:** Pendiente — entregada por Claro en la integración
- **Headers:** `Content-Type: text/xml;charset=UTF-8` + `SOAPAction: ""`
- **Namespace:** `http://service.claro.com.ec/`
- **Operación WSDL:** `OnMensaje` (wrapper SOAP que contiene el XML de negocio)
- **La función** (validateRechargeRetail / processRechargeRetail) va en el atributo `function` del XML body, NO en la URL
- **Flujo CASH_IN:** `PRECHECK(validateRechargeRetail)` → `EXECUTE(processRechargeRetail)`
- **Sin VERIFY ni REVERSE documentados** — idempotencia vía `EXTERNALTRANSACTIONID` único (= uuid)
- **CRÍTICO:** `AUTHORIZATIONNUMBER` del PRECHECK response → enviarlo en el EXECUTE
- **OmniStack debe transformar JSON→XML en EXECUTE y XML→JSON en todos los responses**
- **OFFERID** se resuelve por `rms_item_code` en la tabla `AD_ITEM_SERVICIO` (ver Excel sheet `AD_ITEM_SERVICIO_CLARO`)

---

## 5. Reglas de negocio transversales

### 5.1 Gestión de sesión de proveedores
- **LN** (BET593, Pega3, Tradicionales): OmniStack debe hacer LOGIN antes del primer PRECHECK y mantener el token activo durante el flujo. Si el token expira, re-login y reintentar.
- **Ecuabet**: `shop` y `token` son parámetros de configuración interna — no vienen del front en ningún momento.
- **Claro**: Sin sesión. Credenciales en variables de entorno por cadena/medio.

### 5.2 Persistencia entre fases
OmniStack debe guardar en contexto de transacción (por `uuid`) los campos que encadenan las fases:

| Proveedor | Campo | Del | Para |
|---|---|---|---|
| BET593 CI | `recargaid` | PRECHECK response | EXECUTE / VERIFY / REVERSE |
| BET593 CI | `serialnumber` | PRECHECK response | EXECUTE / VERIFY / REVERSE |
| BET593 CO | `ordenPagoId` | EXECUTE response | VERIFY / REVERSE |
| PEGA3 | `gameTicketNumber` | CREATE_TICKET response | EXECUTE / VERIFY / REVERSE |
| PEGA3 | `drawNumber` | PRECHECK response | CREATE_TICKET request |
| TRADICIONALES | `ventaId` | EXECUTE response | VERIFY / REVERSE |
| CLARO | `AUTHORIZATIONNUMBER` | PRECHECK response | EXECUTE request |

### 5.3 Lógica automática de VERIFY BET593
```
si (response.transaction_state == "ROLLBACK"):
    ejecutar automáticamente POST /v1/reverse
    responder al front con is_error: true
sino:
    responder al front con is_error: false
```

### 5.4 Transformación XML ↔ JSON para Claro
- El front envía JSON a OmniStack
- OmniStack transforma a XML SOAP antes de llamar a Claro
- OmniStack parsea el XML response de Claro y retorna JSON al front
- El XML va envuelto en `<soapenv:Envelope><soapenv:Body><ser:OnMensaje><ser:mensaje><![CDATA[...]]></ser:mensaje>`

### 5.5 Campo `motivo` en REVERSE
- OBLIGATORIO para todos los proveedores
- OmniStack puede usar un texto fijo configurable por proveedor (ej: `"Error en transacción"`)
- LN BET593: campo `motivo` directo en el body
- LN TRAD: campo `motivo` en el body de AnularVentaBoletos
- Ecuabet: no tiene campo motivo en la spec — ignorar o no enviar

### 5.6 Control de cupos diarios CASH_OUT

OmniStack implementa un control de cupos maximos diarios de retiro por local (farmacia) para servicios con `MovementType.CASH_OUT`.

- `MONTO_MAX` de `AD_SERVICIO_PARAMETROS` es el cupo maximo diario por local y tambien el maximo por transaccion.
- **PRECHECK**: Reserva cupo (estado `RESERVADO`). Si no hay cupo disponible, responde error de negocio (HTTP 422).
- **EXECUTE**: Confirma la reserva (estado `CONFIRMADO`).
- **REVERSE mismo dia**: Restituye cupo (estado `REVERTIDO`). Si es otro dia, no se restituye.
- **Timeout** (configurable, default 30 min): Reservas no confirmadas se expiran automaticamente (estado `EXPIRADO`).
- Tabla: `TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO`
- Variables: `APP_CASHOUT_QUOTA_RESERVATION_TIMEOUT_MINUTES`, `APP_CASHOUT_QUOTA_EXPIRATION_SCHEDULER_RATE_MS`

### 5.7 Control de monto maximo/minimo por transaccion (NO-CASH_OUT)

Para todos los servicios que NO son `CASH_OUT`, OmniStack valida que el monto de cada transaccion este dentro del rango [`MONTO_MIN`, `MONTO_MAX`] configurado en `AD_SERVICIO_PARAMETROS` para el item.

- Se ejecuta en **PRECHECK** y **EXECUTE** antes de invocar al proveedor externo.
- Si el monto excede `MONTO_MAX`, responde error de negocio (HTTP 422): _"El monto de la transaccion ($X) excede el maximo permitido ($Y) para el item Z"_.
- Si el monto es inferior a `MONTO_MIN`, responde error de negocio (HTTP 422): _"El monto de la transaccion ($X) es inferior al minimo permitido ($Y) para el item Z"_.
- Este control es solo por transaccion individual — no involucra acumulados ni cupos diarios.
- Los items `CASH_OUT` quedan excluidos de esta validacion porque ya tienen su propio control de monto dentro de `CashOutQuotaService`.
- Clase responsable: `TransactionAmountValidationService`

### 5.7 Variables de entorno por proveedor
Consultar el archivo `OmniStack-TST_postman_environment_v3.json` para los valores de desarrollo. Las variables críticas son:

```
# LN (BET593 / Pega3 / Tradicionales)
LN-UrlBase, LN-Usuario, LN-Clave, LN-Medio (23), LN-ClienteId (58542)
LN-PuntooperacionId, LN-DevideId

# ECUABET
EB-UrlBase, EB-Token, EB-ShopID

# CLARO
CLARO-UrlBase, CLARO-CompanyId (2 dev/Fybeca), CLARO-Username (PRUEBAREC)
CLARO-Password (5566), CLARO-MediaId (RETA), CLARO-Terminal
CLARO-CodCaja (DA00004), CLARO-CodSite (10000004)
CLARO-ConsumerId (FYBECA_001), CLARO-Token (FYBECA)
CLARO-OfferId-DatosEntero (132), CLARO-OfferId-DatosFraccion (150)
CLARO-Latitude, CLARO-Longitude, CLARO-Canton, CLARO-Province, CLARO-Parish
```

---

## 6. Catálogos y constantes

### Categorías
```
1 = ENTRETENIMIENTO   (Apuestas, Boletos)
2 = CORRESPONSAL BANCARIO
3 = SEGUROS
4 = RECAUDOS
5 = TELEFONÍA         (Recargas Claro)
```

### channel_POS
```
POS | WEB | APP | CCT
```

### Capabilities disponibles por proveedor
```
ECUABET:        PRECHECK, EXECUTE, REVERSE
LN BET593:      PRECHECK, EXECUTE, VERIFY, REVERSE
LN PEGA3:       PRECHECK, CREATE_TICKET, EXECUTE, VERIFY, REVERSE
LN TRAD:        PRECHECK, EXECUTE, VERIFY, REVERSE
CLARO:          PRECHECK, EXECUTE
```

### status.code OmniStack
```
00 = OK
01 = Error
02 = Usuario inválido
```

---

## 7. Estructura de directorios recomendada para el microservicio

```
omnistack/
├── CLAUDE.md                          ← este archivo
├── docs/
│   ├── MapeoCampos_v8.xlsx            ← LEER ANTES de tocar providers
│   ├── Documento_Tecnico_V02.docx     ← spec de endpoints
│   ├── OmniStack_postman_v8.json      ← contratos con ejemplos
│   ├── OmniStack_environment_v3.json  ← variables de entorno
│   └── OmniStack_Diagramas_Azul.drawio
├── src/
│   ├── api/
│   │   ├── business-lines.js/ts       ← GET catálogo de servicios
│   │   ├── precheck.js/ts             ← enruta al provider correcto
│   │   ├── create-ticket.js/ts        ← solo LN Pega3
│   │   ├── execute.js/ts
│   │   ├── verify.js/ts
│   │   └── reverse.js/ts
│   ├── providers/
│   │   ├── ecuabet/
│   │   │   ├── precheck.js/ts         ← BuscarUsuario / ConsultaNotaRetiro
│   │   │   ├── execute.js/ts          ← Deposito / NotaRetiro
│   │   │   └── reverse.js/ts
│   │   ├── ln-bet593/
│   │   │   ├── login.js/ts            ← gestión de sesión
│   │   │   ├── precheck.js/ts
│   │   │   ├── execute.js/ts
│   │   │   ├── verify.js/ts           ← lógica COMMIT/ROLLBACK automática
│   │   │   └── reverse.js/ts
│   │   ├── ln-pega3/
│   │   │   ├── login.js/ts
│   │   │   ├── precheck.js/ts         ← 2 llamadas LN
│   │   │   ├── create-ticket.js/ts
│   │   │   ├── execute.js/ts
│   │   │   ├── verify.js/ts           ← GenerarComprobantePega
│   │   │   └── reverse.js/ts
│   │   ├── ln-tradicionales/
│   │   │   ├── login.js/ts
│   │   │   ├── precheck.js/ts         ← 4 llamadas LN
│   │   │   ├── execute.js/ts
│   │   │   ├── verify.js/ts           ← GenerarComprobanteVenta
│   │   │   └── reverse.js/ts
│   │   └── claro/
│   │       ├── soap-client.js/ts      ← transformación JSON↔XML
│   │       ├── precheck.js/ts         ← validateRechargeRetail
│   │       └── execute.js/ts          ← processRechargeRetail
│   ├── router/
│   │   └── provider-router.js/ts      ← decide proveedor por service_provider_code
│   ├── session/
│   │   └── session-store.js/ts        ← persistencia de recargaid, serialnumber, etc.
│   ├── mappers/
│   │   ├── ecuabet.mapper.js/ts       ← transforma OmniStack ↔ Ecuabet
│   │   ├── ln-bet593.mapper.js/ts
│   │   ├── ln-pega3.mapper.js/ts
│   │   ├── ln-tradicionales.mapper.js/ts
│   │   └── claro.mapper.js/ts         ← incluye JSON↔XML
│   └── config/
│       └── providers.config.js/ts     ← URLs, credenciales por entorno
└── tests/
    └── mocks/                         ← usar los JSON de docs/ como fixtures
```

---

## 8. Lógica del router de proveedores

El microservicio determina a qué proveedor llamar en base a `service_provider_code` del request:

```
service_provider_code = 1  → ECUABET
service_provider_code = 2  → LOTERÍA NACIONAL
  └─ sub-routing por rms_item_code:
     rms_item_code empieza con "200..." → LN PEGA3
     rms_item_code empieza con "100..." → LN TRADICIONALES o LN BET593
     ← confirmar con tabla AD_PROVEEDOR_SERVICIO en Tablas_Administración.xlsx
service_provider_code = 7  → CLARO
```

**Para determinar el proveedor exacto de LN** (BET593 vs Pega3 vs Tradicionales), consultar el catálogo de `capabilities` en el response de `/business-lines`. La capability `CREATE_TICKET` solo aparece en LN Pega3.

---

## 9. Pendientes que requieren acción externa antes del desarrollo

Estos puntos NO se pueden resolver con código — requieren respuesta de los proveedores:

1. **CLARO — OFFERID de producción:** Los valores actuales (132, 150, 136) son de desarrollo. Confirmar con Claro antes del go-live.
2. **CLARO — URL de producción:** `CLARO-UrlBase` de producción pendiente de entrega por Claro.
3. **CLARO — IP whitelist:** El servidor de Claro valida la IP del llamante. La IP del servidor de OmniStack debe ser registrada por Claro.
4. **CONCILIATE:** Ningún proveedor tiene endpoint de conciliación documentado. Confirmar con LN, Claro y Ecuabet.
5. **withdrawId vs withdrawalId:** Inconsistencia en la spec de /v1/preCheck sección 2.1 del documento técnico. Definir el nombre canónico.
6. **LN RASPADITAS:** No está en scope inicial pero el Postman tiene los endpoints. Documentar en siguiente fase.

---

## 10. Cómo usar los mocks JSON en los tests

Los archivos `BET593_01_Request-*.json`, `03_Request-*.json`, etc. son fixtures listos para usar en tests de integración. La nomenclatura es:

```
{proveedor/secuencia}_{paso}_{Request|Response}-{FASE}-{PROVEEDOR}-{MOVIMIENTO}.json
```

Ejemplo de uso en test:
```javascript
const req = require('./docs/BET593_01_Request-PRECHECK-RecargarBet593-CASH_IN.json');
const expectedRes = require('./docs/BET593_01_Response-PRECHECK-RecargarBet593-CASH_IN.json');
// El mapper debe transformar la llamada del front (mock con campos OmniStack) 
// en el body que espera el proveedor LN BET593
```

---

## 11. Convenciones de desarrollo

- **Los campos internos** (token, shop, credenciales, medioId, clienteId, etc.) NUNCA provienen del front. Se inyectan desde la configuración del microservicio.
- **El campo `uuid`** es el identificador de trazabilidad — debe propagarse a todos los proveedores como `codigotrn` (LN), `EXTERNALTRANSACTIONID` (Claro) o `customerSessionId` (Pega3).
- **Todos los amounts** tienen formato `double(11.2)`: dos decimales, punto como separador, sin separador de miles. Ej: `1000.00`, `1250.51`.
- **El campo `authorization`** en el response OmniStack siempre contiene el ID de seguimiento externo del proveedor, independientemente del nombre que tenga en cada proveedor (`recargaid`, `transactionId`, `gameTicketNumber`, `ventaId`, `AUTHORIZATIONNUMBER`, `ordenPagoId`).
- **Claro SOAP:** El XML va dentro de `<![CDATA[...]]>` dentro del elemento `<ser:mensaje>` del wrapper SOAP.

---

## 12. Referencia rápida de campos por proveedor

> Para el mapeo completo campo por campo ver `MapeoCampos_v8.xlsx` sheets: PRECHECK, EXECUTE, CREATE_TICKET, VERIFY, REVERSE.

### BET593 — Traducción de campos clave
| OmniStack | LN BET593 | En qué fase |
|---|---|---|
| uuid | codigotrn | PRECHECK req |
| document | cuentaweb | PRECHECK req / res |
| amount | valor | PRECHECK req / res |
| authorization | recargaid | PRECHECK res ★ guardar |
| serialnumber | serialnumber | PRECHECK res ★ guardar |
| authorization | ordenPagoId | EXECUTE CO res ★ guardar |
| is_error | codError == 0 | todos |
| error.code | codError | todos |
| error.message | msgError | todos |
| status.code | resultado | todos |
| transaction_state | estado | VERIFY res |

### CLARO — Traducción de campos clave
| OmniStack | Claro XML | En qué fase |
|---|---|---|
| uuid | EXTERNALTRANSACTIONID | ambas |
| phone | SUBSCRIBERID (593+phone) | ambas |
| amount | QUANTITY | ambas |
| authorization | AUTHORIZATIONNUMBER | PRECHECK res ★ / EXECUTE req |
| is_error | ID_CODE != 0 | ambas |
| error.code | ID_CODE | ambas |
| status.message | SYSTEMMESSAGE | ambas |

### PEGA3 — Traducción de campos clave
| OmniStack | LN Pega3 | En qué fase |
|---|---|---|
| active_draw.draw_number | drawNumber | PRECHECK res ★ → CREATE_TICKET |
| ticket_data.draw_number | noOfDraws | CREATE_TICKET req |
| ticket_data.entry_type | entryType | CREATE_TICKET req |
| ticket_data.panels[].numbers | value[] | CREATE_TICKET req |
| ticket_data.panels[].play_types | playTypes[] | CREATE_TICKET req |
| authorization | gameTicketNumber | CREATE_TICKET res ★ guardar |
| comprobante_b64 | base64 | VERIFY res |

---

*Documento generado el 13-May-2026. Actualizar este archivo cuando cambien contratos de proveedores o se agreguen nuevos endpoints.*

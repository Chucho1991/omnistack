# Contexto de sesión — Tradicionales/Claro (2026-07-05 a 2026-07-07)

Rama: `feature/HU-13052026`. Este documento resume el trabajo de una sesión larga de debugging e
implementación sobre la integración de **LN Tradicionales** (Lotería/Lotto/Pozo Millonario) y
**Claro**, para que se pueda retomar el hilo desde otra máquina/terminal sin perder contexto.

---

## 1. Patrón general encontrado esta sesión

Varios adaptadores de proveedores se construyeron a partir de `docs/OmniStack_postman_collection_v8.json`
(mocks nunca verificados contra el proveedor real), y varios nombres de campo ahí resultaron estar
mal. La lección repetida: **cuando un flujo de proveedor falla con "parámetro requerido" o trae
campos `null` pese a `codError:0`/200 OK, sospechar primero de un mismatch de nombre de campo o de
un valor que nunca se pobló — no asumir que es un problema de datos/ambiente.** Verificar contra
`docs/LOTERIA NACIONAL.docx` (LN) o `docs/document_pdf.pdf` (Claro) extrayendo el texto real, en vez
de confiar en los mocks viejos.

Herramientas usadas para extraer texto de docs binarios en esta sesión:
- `.docx`: descomprimir como zip y parsear `word/document.xml` (ver técnica en el historial; para
  ver estructura de tabla completa, reemplazar `</w:tr>`/`</w:tc>`/`</w:p>` por separadores antes de
  quitar tags — el texto corrido sin esto puede ocultar una columna).
- `.pdf`: **`pdftotext`** SÍ está disponible en `C:\Users\usuario\AppData\Local\Programs\Git\mingw64\bin\pdftotext.exe`
  (viene con Git for Windows) — no hace falta instalar poppler.
- `.xlsx`: **Microsoft Office SÍ está instalado** (`C:\Program Files\Microsoft Office\root\Office16\EXCEL.EXE`).
  No hay Python/openpyxl. Para editar `.xlsx` de forma segura, usar automatización COM desde
  PowerShell (`New-Object -ComObject Excel.Application`) en vez de manipular el XML interno a mano
  (frágil, sin `sharedStrings.xml`, todo `t="inlineStr"`).

---

## 2. Bugs de Claro encontrados y corregidos

- **`ClaroXmlAdapter.parseResponse`**: leía `ID_CODIGO`, pero el proveedor usa **`ID_CODE`** en
  `processRechargeRetail` (EXECUTE) y **`ID_CODIGO`** en `validateRechargeRetail` (PRECHECK) —
  inconsistente entre operaciones del mismo proveedor. Esto hacía que **toda** transacción de Claro
  se marcara como error aunque el proveedor dijera "se realizó con éxito". Fix: acepta ambos
  (`fields.getOrDefault("ID_CODE", fields.getOrDefault("ID_CODIGO", ""))`).
- **`CHANNELID`**: el spec real confirma que el campo se llama literalmente `CHANNELID` (no
  `PV_CHANNELID` — se probó y revirtió).
- **`ProviderConfigService.getProviderProperties()`** nunca poblaba los campos CLARO-specific
  (`consumerId`, `channelId`, `mediaId`, `mediaDetailId`, `subscriberType`, `subscriptionType`) —
  viven en `IN_OMNI_PROVEEDOR_WS_DEFS`, no en `IN_OMNI_PROVEEDOR_CONFIG`. Se resolvieron en
  `ClaroExecuteStrategy` vía `ProviderWsDefsService`, igual patrón que ya usaba `ClaroPrecheckStrategy`
  para `mediaId`. Se agregó `docs/bdd/omnistack/24_FIX_CLARO_EXECUTE_WS_DEFS.sql` (falta correr en BD).

---

## 3. Bugs de LN Tradicionales encontrados y corregidos

- **Figuras vacías**: `TradicionalFigurasQueryResponse.Figura` no tenía `@JsonProperty` — el
  proveedor real usa `codigoImagen`/`descripcionImagen`/`abreviaturaImagen`, no
  `figuraId`/`nombre`/`descripcion`. Corregido.
- **`draws[].disponible`** y **`available_numbers[].disponible`/`.precio`**: campos fabricados sin
  respaldo en el spec real — siempre venían `null`. Eliminados. El precio real vive en
  `draws[].precio` (por sorteo, `pvp`), no por número individual.
- **`reserva_id` (bug crítico)**: `RecuperarNumerosDisponiblesPorCombinacion` devuelve `numeroReserva`,
  que **nunca se capturaba** (campo ausente del DTO). Mientras tanto, `VentaBoletos.reservaId` se
  mandaba con el `uuid` propio de OmniStack — un valor que el proveedor nunca generó. Fix: se agregó
  `numeroReserva` a `TradicionalNumerosQueryResponse`, se expone como `CreateTicketResponse.reserva_id`,
  y `ExecuteRequest.reserva_id` (nuevo, **obligatorio**) reemplaza el uso del `uuid`.
- **`fracciones_vendidas`**: `VentaBoletos` devuelve `ListaR[].listaNumeroFracciones[].numeroFraccion`
  (qué fracción(es) específicas le tocaron al cliente) — no se capturaba. Se agregó y expone en
  `ExecuteResponse.fracciones_vendidas`. Nota: el segundo registro de ese arreglo puede traer el
  código de mascota (solo Pozo Millonario) — por eso solo se toma el primero.
- **`cantidad_fracciones`**: `TradicionalCreateTicketStrategy` tenía `cantidad(0)` hardcodeado. Se
  agregó `CreateTicketRequest.cantidad_fracciones`. Confirmado contra el spec: `cantidad` es
  literalmente "cantidad de fracciones solicitadas" — no un número de slot específico. El flujo real
  es de **2 llamadas** a CREATE_TICKET: 1) explorar sin combinación (`combinacion:""`) para ver
  `available_numbers[].fracciones` disponibles, 2) reservar con `combinacion` = número elegido +
  `cantidad_fracciones`.
- **Inconsistencia `game_id`/`draw_id` vs `juego_id`/`sorteo_id`**: `CreateTicketResponse.TradicionalNumber`
  usaba nombres distintos a PRECHECK/EXECUTE para el mismo concepto (bug nuestro, no del proveedor).
  Unificado a `game_id`/`draw_id` en los 3 endpoints.
- **`comprobante_b64` → `comprobante_url`** (cambio de contrato, breaking, decidido con el usuario):
  además, `TradicionalWebClientAdapter.generateComprobante` trataba la respuesta HTTP de
  `GenerarComprobanteVenta` como si fueran los bytes crudos del PDF, pero el proveedor envuelve el
  PDF en JSON (`{fileName, contentType, base64}`). Se corrigió el parseo y se agregó
  `ComprobanteStoragePort` + `LocalDiskComprobanteStorageAdapter` (disco local, path configurable
  `app.comprobantes.storage-path`) + `ComprobanteUrlService` + `GET /v1/comprobantes/{id}` para
  servir el archivo.
- **`games` (campo eliminado)**: `PrecheckResponse.games`/`TradicionalGame` se quitaron por completo
  — decisión del usuario, no se usaba en los flujos siguientes (CREATE_TICKET/EXECUTE ya reciben el
  juego específico vía `rms_item_code`). La llamada a `RecuperarJuegosPorMedio` se mantiene solo para
  determinar éxito/error del PRECHECK.
- **PRECHECK ahora valida datos mínimos**: si `RecuperarSorteosDisponibles` responde `codError:0`
  pero `listaDetalle:null` (visto repetidas veces, para distintos juegos/sorteos — parece ser estado
  normal del proveedor cuando no hay sorteo abierto), `LoteriaTradicionalPrecheckStrategy` ahora
  lanza `IntegrationException` (HTTP 502) en vez de devolver `is_error:false` con `draws` vacío. Para
  Pozo Millonario (`juegoId=5`), lo mismo si `figures` viene vacío (necesita mascotas).

### ⚠️ Cambios de contrato BREAKING que el front (GEOPos/caja) todavía no recibió
- `comprobante_b64` → `comprobante_url`
- `CreateTicketResponse.available_numbers[].juego_id`/`sorteo_id` → `game_id`/`draw_id`
- `PrecheckResponse.games` eliminado
- `CreateTicketRequest.cantidad_fracciones` (nuevo, opcional)
- `ExecuteRequest.reserva_id` (nuevo, **obligatorio** para Tradicionales — sin él, EXECUTE lanza error)

---

## 4. Pendientes reales para el correo a Lotería Nacional

Solo quedan **2 preguntas genuinas** que no se pueden resolver sin respuesta externa:

1. **Reverse/Anulación sin respuesta ni número de autorización**: si el EXECUTE falla sin respuesta
   del proveedor, no hay `authorization`/`ventaId` para poder anular. Requiere que negocio/Lotería
   definan un proceso operativo (no es un bug de código).
2. **Selección de varias mascotas en Pozo Millonario**: RF-06 de negocio pide elegir varias, pero
   `combinacionFigura` está documentado como `String` singular ("se podrá seleccionar **la**
   figura"), sin ningún ejemplo de múltiples valores. Falta que Lotería confirme si el campo acepta
   CSV/array real (aunque no esté documentado), o si hay que resolverlo con N llamadas.

Todos los demás puntos del correo original (11 en total) ya están resueltos e implementados — no
hace falta re-preguntarlos. Ver historial de conversación o pedir el detalle completo si hace falta
el desglose punto por punto.

---

## 5. Estado de `docs/MapeoCampos_v8.xlsx`

Actualizado completo (2026-07-06/07) en las 4 hojas transaccionales de Tradicionales:
- **PRECHECK**: reemplazado el `game_catalog[]` viejo (mal ubicado, nunca existió en el código real)
  por `draws[]`/`figures[]` reales.
- **CREATE_TICKET**: se agregó la columna "LN TRAD" completa (no existía en absoluto).
- **EXECUTE**: agregado `cantidad_boletos`, `reserva_id`, `fracciones_vendidas`; corregido typo
  `"cl"`→`"clave"`.
- **VERIFY**: `comprobante_b64`→`comprobante_url`.

Ver `docs/MapeoCampos_TRADICIONALES_correcciones.md` para el detalle campo por campo (útil como
referencia rápida sin abrir el Excel). **Este Excel es un documento vivo** — cada mapeo nuevo que se
confirme por pruebas debe reflejarse ahí también, no solo en código.

---

## 6. Otros hallazgos operativos

- **Ambiente de pruebas de La Lotería (juegoId=1)**: mostró repetidamente `listaDetalle:null` en
  varios endpoints (figuras, sorteos, números) mientras Lotto/Pozo sí traían datos reales con el
  mismo código — parece ser falta de inventario de prueba sembrado para ese juego específico en el
  ambiente de homologación del proveedor, no un bug. El usuario dio esto por resuelto/entendido.
- **Auto-commit/push detectado**: hay un mecanismo en el entorno del usuario que hace commit+push
  automático en cada guardado, con el mensaje genérico "correcion de imagenes" — ya se le avisó al
  usuario. Al momento de escribir esto, la rama local iba **1 commit adelante de origin** (el commit
  del Excel) — confirmar que se haya subido antes de trabajar desde otra máquina.
- **Precaución de encoding en PowerShell**: un reemplazo global con `Get-Content -Raw` +
  `[System.IO.File]::WriteAllText` sin especificar encoding UTF-8 explícito corrompió acentos en
  `docs/OmniStack_postman_collection_v10.json` (mojibake tipo "TransacciÃ³n"). Se corrigió
  reinterpretando el texto (Windows-1252→bytes→UTF-8). Siempre usar `-Encoding UTF8` en
  `Get-Content` y `New-Object System.Text.UTF8Encoding($false)` al escribir con archivos que tengan
  texto en español con tildes/ñ.

---

## 7. Postman v10

`docs/OmniStack_postman_collection_v10.json` actualizado en paralelo con todos los cambios de
contrato de arriba (ejemplos de PRECHECK sin `games`, CREATE_TICKET con `cantidad_fracciones` y
`reserva_id`, EXECUTE con `reserva_id` obligatorio, `game_id`/`draw_id` consistentes). Validado con
`ConvertFrom-Json` en cada edición para asegurar que sigue siendo JSON válido.


---

## 8. Fix BET593 PRECHECK CASH_OUT — subcategory_code (2026-07-14)

**Problema:** El request de PRECHECK CASH_OUT con `subcategory_code=1121` fallaba con:
`"La configuracion de Loteria BET593 no define el valor requerido para subcategory_code"`

**Causa raíz:** `LoteriaBet593WithdrawPrecheckStrategy.validateBusinessContext()` validaba
`subcategory_code` contra `IN_OMNI_PROVEEDOR_CONFIG` (clave `loteria|subcategory_code`), pero esa
tabla solo puede almacenar **un** valor para el proveedor `'loteria'`. Los scripts de configuración
(`04_UPDATE_PROVEEDOR_CONFIG_QA.sql`) lo dejaron en `1120` (CASH_IN). El CASH_OUT usa `1121`, por
lo que la validación siempre fallaba (o si no se ejecutó el script 04, el valor era `null`).

**Fix aplicado:** Se eliminaron las 2 llamadas a `validateValue("subcategory_code", ...)` del método
`validateBusinessContext()` en `LoteriaBet593WithdrawPrecheckStrategy`. Esto es consistente con:
- El comentario del script `04_UPDATE_PROVEEDOR_CONFIG_QA.sql` que dice "subcategory_code no es
  validado por las strategies de BET593 (el routing por 4 campos en el catálogo ya lo garantiza)"
- El hecho de que business lines (`AD_SERVICIO_PARAMETROS` + `SUBCLASS`) ya trae `1121` correctamente
  y el routing por `(category_code, subcategory_code, service_provider_code, rms_item_code)` ya
  garantiza que la request llega a la strategy correcta

**Archivos modificados:**
- `src/main/java/.../LoteriaBet593WithdrawPrecheckStrategy.java` — eliminada validación
- `README.md` — corregidos códigos de catálogo BET593 (eran placeholders 759/161/2, ahora 983/1120|1121/408403)
- `docs/OmniStack_postman_collection_v10.json` — ya tenía valores correctos, sin cambios necesarios

---

## 9. Pozo Millonario/Revancha — datos completos en PRECHECK/CREATE_TICKET + bugs reales de VentaBoletos y Pega3 EXECUTE (2026-07-14/15)

Sesión de pruebas en vivo contra QA (Windows → QA Oracle) sobre Lotería Nacional Tradicionales
(Pozo Millonario + Revancha) y Pega3. Patrón repetido: **el `.docx` documenta nombres de campo
que no siempre coinciden con lo que el proveedor realmente manda en QA** — cada hallazgo de esta
sección se verificó contra logs reales, no solo contra el spec.

### 9.1 PRECHECK — `draws[]` traía solo 9 de ~24 campos documentados

`TradicionalSorteosQueryResponse.Sorteo` no capturaba casi nada de la metadata que Pozo
Millonario necesita para armar su UI (a diferencia de Lotería/Lotto, que solo usan un número
simple). Se agregaron y ahora se exponen en `PrecheckResponse.TradicionalDraw`:

- `nombre_segunda_combinacion` / `nombre_tercera_combinacion` / `nombre_cuarta_combinacion` /
  `nombre_quinta_combinacion` — etiquetas de cada parte adicional de la combinación (en Pozo:
  `"Combinación 10/25 (Pozo Millonario)"` y `"Mascota 8 Pozo"` — esta última es la señal de que
  el sorteo requiere seleccionar figura/mascota).
- `cantidad_digitos_combinacion_principal` / `_secundaria` — cuántos dígitos debe pedir el front.
- `nombre_numero`, `tiene_premio_instantaneo`, `tipo_premio_primera_suerte`,
  `nombre_primera_suerte`, `se_acumula`, `monto_proximo_sorteo`, `es_sorteo_destacado`, `clase`,
  `nombre_sala_sorteo`, `nombre_juego`, `fecha_cierre_ventas`, `fecha_caducidad_sorteo`.

Nada de esto afecta routing ni otras strategies — es aditivo puro sobre el mapeo de campos.

### 9.2 CREATE_TICKET — `numero2/3/4/5` se descartaban por completo

`TradicionalNumerosQueryResponse.Numero` ya capturaba `numero2`, `numero3`, `numero4`, `numero5`,
`cantidad` y `reserva` desde el proveedor, pero `TradicionalCreateTicketStrategy.mapNumeros()`
solo copiaba `numero` al `CreateTicketResponse.TradicionalNumber`. Para Pozo Millonario esto era
un bug real: el proveedor manda el cartón (`numero`, 7 dígitos) y la combinación secundaria
(`numero3`, 2 dígitos) como dos partes del mismo boleto — `numero3` se perdía siempre. Se agregó
el mapeo completo de `numero2/3/4/5` + `cantidad`/`reserva` (fracciones realmente reservadas,
puede diferir de lo solicitado).

### 9.3 VentaBoletos response — bug crítico y silencioso: nombres reales ≠ `.docx`

El proveedor real en QA responde `listaVentaSuerte` / `listaVentaSorteos` / `listaNumerosVendidos`,
pero `.docx` documenta (y el código usaba) `listaSUE` / `listaSorteos` / `ListaR`. Como
`TradicionalVentaBoletosResponse` tiene `@JsonIgnoreProperties(ignoreUnknown = true)` sin alias,
Jackson nunca tiraba error — el campo simplemente quedaba `null` **siempre**, aunque la venta
fuera 100% exitosa (`codError:0`, `ventaId` generado). Consecuencia: `boleto_clave`, `boleto_qr`
y `fracciones_vendidas` nunca llegaban al POS pese a que el proveedor sí los mandaba.

Fix: `@JsonProperty` corregido a los nombres reales + `@JsonAlias` a los nombres del `.docx` como
respaldo (mismo patrón que ya se usó para `fechaCierreVenta` singular vs `fechaCierreVentas`
plural documentado — mismo tipo de mismatch, corregido igual).

De paso se agregó captura de datos que el proveedor ya mandaba y se ignoraban:
- `boleto_id` (liga Pozo Millonario con su Revancha cuando comparten valor)
- `valor_total_vendido`
- `fracciones_vendidas_detalle` — lista completa de `listaNumeroFracciones`. Antes solo se leía
  el primer elemento (`fracciones_vendidas`, CSV de la combinación); para Pozo Millonario el
  **segundo** elemento es la mascota vendida (ej. `"Mascota 10 (Gato)"`) y se estaba descartando.

**Archivos**: `TradicionalVentaBoletosResponse.java`, `TradicionalWebClientAdapter.java`,
`ExecuteResponse.java` (nuevo `SoldFraction`), `LoteriaTradicionalExecuteStrategy.java`.

### 9.4 Pega3 EXECUTE (CASH_IN) llamaba mal a `PagarTicket` — `"Invalid Barcode"`

Bug de arquitectura ya sospechado en el plan de CASH_OUT de premios (sección "fuera de alcance"),
confirmado hoy con un error real: `LoteriaPega3ExecuteStrategy` (EXECUTE CASH_IN, venta de un
ticket) llamaba a `PagarTicket` — pero ese endpoint es para **cobrar el premio de un ticket YA
vendido y ganador**, no para confirmar una compra. Al pasarle el ticket recién creado por
`CrearTicket` (que aún no es ganador ni ha sido jugado), el proveedor respondía
`"message":"Invalid Barcode"`.

Confirmado contra el spec: la respuesta de `CrearTicket` ya trae `"status":"Purchased"` — la
venta queda completa ahí mismo. No existe un endpoint separado de "confirmar venta" para Pega3
(a diferencia de Tradicionales, que reserva en `RecuperarNumerosDisponiblesPorCombinacion` y
vende en `VentaBoletos` como 2 pasos distintos).

**Fix**: `LoteriaPega3ExecuteStrategy` ya no llama a ningún endpoint del proveedor. Solo valida
`authorization`+`amount` y responde éxito usando esos mismos valores (el `ticketNumber` que el
POS reenvía desde CREATE_TICKET). `Pega3PayTicketPort`/`PagarTicket` se mantiene intacto — sigue
en uso, pero exclusivamente en `LoteriaPega3CashOutExecuteStrategy` (pago de premios, CASH_OUT),
que es el flujo correcto para ese endpoint.

### 9.5 Pendiente sin resolver — `transaccion` para el comprobante PNG de Pega3 VERIFY

`GenerarComprobantePega` exige `ventaId` + `idUsuario` + `transaccion` (los 3 obligatorios según
el spec), pero ni `CrearTicket` ni `ConsultarTicket` devuelven ese `transaccion` en ningún
response documentado. Hoy el comprobante PNG de Pega3 solo se genera si el POS manda
`transaccion` explícito en el body de `/v1/verify` (`VerifyRequest.transaccion`) — si no lo
manda, `comprobanteUrl` sale `null`, sin error.

Dado el patrón de esta sesión (9.3 y el fix de `fechaCierreVenta`), es muy probable que la
respuesta REAL de QA de `ConsultarTicket`/`CrearTicket` traiga este dato bajo otro nombre no
documentado. **Falta un log real de QA de esos dos endpoints para confirmar o descartar**.

**Bug encontrado al intentar verificarlo**: `Pega3WebClientAdapter.invokePega3()` parseaba el
body a la clase `T` (`Pega3VerifyTicketResponse`, etc.) **dentro** de la cadena reactiva, y
logueaba (tanto a consola como a `IN_OMNI_LOGS_WS_EXT`) la re-serialización de ese DTO ya
recortado — no el JSON crudo del proveedor. Cualquier campo no modelado en nuestros DTOs de
Pega3 (como un eventual `transaccion`) se perdía silenciosamente antes de llegar a cualquier log,
haciendo imposible depurarlo así se pidiera el log 10 veces. `TradicionalWebClientAdapter` no
tiene este problema — ya loguea el string crudo antes de parsear (`invokePost(String)`), por eso
ahí sí pudimos encontrar `listaVentaSuerte` etc. en 9.3.

✅ Corregido: `invokePega3()` ahora captura el body crudo (`bodyToMono(String.class)`), lo loguea
tal cual (consola + `IN_OMNI_LOGS_WS_EXT`) y recién después lo parsea a `T` — mismo patrón que
Tradicionales. Aplica a **todas** las llamadas de Pega3 (VentaProductos, ObtieneSorteosActivo,
CrearTicket, PagarTicket, ConsultarTicket, CancelarTicket), no solo VERIFY.

**Resultado con el fix ya desplegado**: con el body crudo real a la vista, se confirmó que
`ConsultarTicket` **no trae `transaccion` bajo ningún nombre** — la respuesta real completa es
`{gameTicketNumber, status, createdOn, cost, mainGame{...}, promotions, entryType, channel}`.
`CrearTicket` tampoco lo trae. Se descarta definitivamente que sea un problema de mapeo — el dato
genuinamente no existe en ninguna respuesta documentada ni real de estos 2 endpoints.

**Hallazgo colateral valioso**: `CrearTicket` sí devuelve `codigoQR` (URL del código QR del
ticket vendido) — campo que **no estaba modelado** en `Pega3CreateTicketResponse.java` y se
descartaba en silencio por `@JsonIgnoreProperties(ignoreUnknown=true)`. `ConsultarTicket` (VERIFY)
nunca lo trae. ✅ Corregido: se agregó `codigoQR` al DTO, se captura en el payload como
`ticket_qr` (`Pega3WebClientAdapter.createTicket()`) y se expone en
`CreateTicketResponse.ticket_qr` (mapeado en `LoteriaPega3CreateTicketStrategy`). Es una URL, no
una imagen — el POS/impresora debe renderizarla como código de barras QR, a diferencia del
`comprobante_url` de Tradicionales que sí es un PNG servido por nosotros.

**Fix del `transaccion` — hipótesis implementada, pendiente de confirmar en QA**: dado que
`transaccion` se documenta como "código de la transacción asociada a la venta", y que OmniStack
ya manda `customerSessionId = uuid` en cada llamada a Pega3 (incluida `CrearTicket`), se agregó
`RegistroTrxPort.findCreateTicketUuidByAuthorization(ticketNumber)` — consulta
`IN_OMNI_REGISTRO_TRX` (que ya guarda `UUID` + `AUTHORIZATION` por cada `CREATE_TICKET`) para
recuperar el `uuid` que se uso como `customerSessionId` al crear ESE ticket especifico, y lo usa
como `transaccion` automáticamente en `LoteriaPega3VerifyStrategy.fetchComprobanteIfAvailable()`
si el POS no lo manda explícito (que sigue teniendo prioridad si viene). **No es un dato
confirmado por el proveedor** — es la hipótesis más plausible con la evidencia disponible, y solo
se puede validar probándola contra QA real. Implementado en `RegistroTrxPort` (+ `OracleRegistroTrxAdapter`
+ `NoOpRegistroTrxAdapter`) y `LoteriaPega3VerifyStrategy`. 126/126 tests ✅. **Pendiente**: correr
el flujo completo (CREATE_TICKET→EXECUTE→VERIFY) y confirmar si `GenerarComprobantePega` acepta
este valor o lo rechaza.

### 9.6 `game_data.entry_types` filtrado a solo `Verbal-*` (aclaración pedida en reunión)

Pregunta de negocio: por qué `entry_types` trae 4 valores (`Playslip-Manual`, `Playslip-QuickPick`,
`Verbal-Manual`, `Verbal-QuickPick`) y cuál debería usar el front. Cruzado contra
`docs/GPFEC-3477 Recaudo Lotería - Negocio.pdf` (RF-07/CU-04, Venta de Pega): el documento
funcional solo describe un flujo — el cajero digita los dígitos o pide "número aleatorio" — nunca
menciona papeleta física de autoservicio. Es decir, el eje `Playslip` vs `Verbal` del proveedor no
tiene ningún equivalente en GEOPos; solo aplica `Verbal` (`Manual`=dígitos, `QuickPick`=aleatorio).

**Fix**: `Pega3WebClientAdapter.queryProduct()` ahora filtra `response.getEntryTypes()` a solo los
que empiezan con `"Verbal-"` antes de construir `entry_types` y antes de tomar el "primero" para
`max_cost`/`future_draws_limit`/`advance_draw_limit`/`play_types` (antes tomaba el primero de la
lista cruda del proveedor, que podía ser un `Playslip-*`). `game_data.entry_types` ahora solo
expone `["Verbal-Manual", "Verbal-QuickPick"]`.

### 9.7 Verificación

`mvn compile` + `mvn test` en verde (126/126) después de cada cambio de esta sección. Sin tests
unitarios dedicados para `LoteriaTradicionalExecuteStrategy`, `TradicionalCreateTicketStrategy`
ni `LoteriaPega3ExecuteStrategy` — por eso el bug de 9.3 nunca se detectó en CI pese a llevar
tiempo en el código. Pendiente si se retoma el patrón de tests del plan de CASH_OUT.

### 9.8 Postman v10

`docs/OmniStack_postman_collection_v10.json` actualizado: ejemplos de PRECHECK/CREATE_TICKET/
EXECUTE de Pozzo Millonario con todos los campos nuevos de 9.1-9.3 (incluye `figura_id` en el
request de CREATE_TICKET y `numero3` real por boleto según el ejemplo del proveedor), ejemplo
de EXECUTE de Pega3 CASH_IN corregido para reflejar 9.4, y `game_data.entry_types` de las 3
carpetas Pega (Pega3/Pega4/Pega2) filtrado a `["Verbal-Manual","Verbal-QuickPick"]` para reflejar
9.6. Changelog completo también agregado a la descripción de la carpeta "📌 NOTAS v10" del propio
JSON. Validado con `json.load` en cada edición.

---

## 10. Migración MAX+1 → secuencias Oracle (2026-07-16)

Se eliminaron todos los patrones anti-patrón `NVL(MAX(PK),0)+1` de los scripts SQL del proyecto,
sustituyéndolos por secuencias Oracle (`SEQUENCE.NEXTVAL`). Esto previene condiciones de carrera
en ambientes con concurrencia y es la práctica estándar Oracle para generación de IDs.

### 10.1 Archivos modificados

| Archivo | Cambio |
|---|---|
| `docs/bdd/omnistack/03_DML_PROVEEDORES_PLSQL.sql` | 4 tipos de MAX+1 reemplazados por `.NEXTVAL FROM DUAL` |
| `docs/bdd/local-setup/06b_GPF_OMNISTACK_DML.sql` | 4 tipos de MAX+1 reemplazados por `.NEXTVAL FROM DUAL` |
| `docs/bdd/local-setup/03_GPF_OMNISTACK_CAMBIOS.sql` | 4 secuencias nuevas creadas (DDL) |
| `docs/bdd/omnistack/ALL_SEQUENCES.sql` | Header actualizado documentando las 13 secuencias totales |
| `src/.../oracle/OracleRegistroTrxAdapter.java` | `NVL(MAX(CODIGO),0)+1` → `SEQ_IN_OMNI_REGISTRO_TRX.NEXTVAL` |
| `src/.../oracle/OracleAuditLogAdapter.java` | `NVL(MAX(CODIGO),0)+1` → `SEQ_IN_OMNI_LOGS_APP.NEXTVAL` |
| `src/.../oracle/OracleWsExtLogAdapter.java` | `NVL(MAX(CODIGO),0)+1` → `SEQ_IN_OMNI_LOGS_WS_EXT.NEXTVAL` |

### 10.2 Secuencias por esquema

**Esquema TUKUNAFUNC** (9 secuencias, ya existían):
1. `SEQ_IN_OMNI_PROVEEDOR_WS` → `IN_OMNI_PROVEEDOR_WS.ID_WS`
2. `SEQ_IN_OMNI_PROVEEDOR_HEADERS` → `IN_OMNI_PROVEEDOR_HEADERS.ID_HEADER`
3. `SEQ_IN_OMNI_PROVEEDOR_WS_DEFS` → `IN_OMNI_PROVEEDOR_WS_DEFS.ID_DEFAULT`
4. `SEQ_IN_OMNI_PROVEEDOR_CONFIG` → `IN_OMNI_PROVEEDOR_CONFIG.ID_CONFIG`
5. `SEQ_IN_OMNI_LOGS_APP` → `IN_OMNI_LOGS_APP.CODIGO`
6. `SEQ_IN_OMNI_LOGS_WS_EXT` → `IN_OMNI_LOGS_WS_EXT.CODIGO`
7. `SEQ_IN_OMNI_REGISTRO_TRX` → `IN_OMNI_REGISTRO_TRX.CODIGO`
8. `SEQ_IN_OMNI_INPUT_FIELDS` → `IN_OMNI_INPUT_FIELDS.ID_FIELD`
9. `SEQ_IN_OMNI_CASHOUT_CUPO` → `IN_OMNI_CASHOUT_CUPO_DIARIO.ID_CUPO`

**Esquema GPF_OMNISTACK** (4 secuencias, nuevas):
10. `SEQ_AD_SERVICIO_PARAMETROS` → `AD_SERVICIO_PARAMETROS.ID_CONFIG`
11. `SEQ_AD_CANAL_SERVICIO` → `AD_CANAL_SERVICIO.COD_CAN_SERV`
12. `SEQ_AD_COM_FORMPAG_SERVICIO` → `AD_COM_FORMPAG_SERVICIO.COD_COM_FORMPAG_SERV`
13. `SEQ_AD_ITEM_SERVICIO` → `AD_ITEM_SERVICIO.COD_ITEM_SERV`

### 10.3 Nota para QA/PROD

En ambientes con datos existentes, las secuencias GPF_OMNISTACK deben crearse con
`START WITH (SELECT MAX(PK)+1 FROM tabla)` para evitar colisión con registros ya insertados.
Las secuencias TUKUNAFUNC (1-9) ya se creaban desde `01_DDL_ESTRUCTURA.sql` / `06_DDL_INPUT_FIELDS.sql` /
`26_DDL_CASHOUT_CUPO_DIARIO.sql`, así que allá solo aplica el ajuste de `START WITH` si se
recrean sobre un ambiente poblado.

---

## 11. Pozo Revancha 1-sola-llamada, PNG directo, comprobante multi-imagen, y Pega3 sin CREATE_TICKET (2026-07-18/19)

Sesión de pruebas en vivo contra QA (Pega3 y Tradicionales), varios hallazgos confirmados
con logs reales del proveedor.

### 11.1 Pozo Millonario + Revancha — una sola llamada de reserva, no dos

Confirmado con el proveedor en reunión directa: `RecuperarNumerosDisponiblesPorCombinacion`
con `juegoId=5` (Pozo Millonario) **ya devuelve anidada la Revancha** (`juegoId=17`) en el
mismo `listaDetalle`, emparejada por el campo `boleto` — nunca hay que llamar de nuevo con
`juegoId=17`. `TradicionalCreateTicketStrategy` hacía exactamente esa segunda llamada
redundante, generando una **reserva descartada** (un `numeroReserva` distinto que nunca se
reenviaba a `VentaBoletos`) — bug real, no solo ineficiencia. ✅ Corregido: una sola llamada
a `queryNumeros` con el `juegoId` principal; `querySorteos` sigue llamándose para ambos
juegos, pero solo para precio (`RecuperarSorteosDisponibles` no tiene efecto secundario).
`mapNumeros()` ahora elige el `precio_unitario` por entrada según su propio `juegoId`, no un
precio único para toda la lista.

### 11.2 `formato` nuevo en GenerarComprobantePega/Venta — PNG directo del proveedor

El spec v0005 (13-May-2026, más reciente que el v0004 usado hasta ahora) agrega el parámetro
`formato` (0=PDF, 1=PNG, 2=JPG) a ambos endpoints de comprobante. Con `formato=1/2` el
proveedor responde una **lista** (`{total, formato, imagenes:[{fileName,contentType,base64}]}`)
en vez del objeto plano de un solo archivo. Se agregó `formato=1` a ambos adapters
(Tradicionales y Pega3) — ya no hace falta nuestra conversión PDFBox salvo que el proveedor
ignore el parámetro y siga mandando PDF (fallback automático, `ComprobanteUrlService` ya
detectaba el content-type real).

### 11.3 `comprobante_url` (string) → `comprobante_urls` (lista) — breaking change

Una venta con varios juegos (Pozo Millonario + Revancha) genera **una imagen de comprobante
por cada juego vendido** (confirmado con QA real: `total=2`, un PNG por `ticket-<ventaId>-N.png`).
El campo singular `comprobante_url` solo alcanzaba para guardar la primera y descartaba el
resto. Se reemplazó por `comprobante_urls` (lista) en `VerifyResponse`, tanto para
Tradicionales como para Pega3 — decisión explícita del usuario de romper el contrato en vez
de mantener compatibilidad. El front (GEOPos) necesita actualizarse.

### 11.4 Pega3 — bug de logging (no se veía la respuesta cruda) + `ventaId` truncado

`Pega3WebClientAdapter` no logueaba el body de respuesta exitosa de `GenerarComprobantePega`
(solo el request y los casos de error) — mismo tipo de bug ya corregido para Tradicionales en
la sección 9. Corregido con captura de raw body + log de resumen redactado (sin el base64
completo, solo tamaños). Con el log real a la vista, se confirmó el bug real: `ventaId` debe
ser el `gameTicketNumber` que devuelve `ConsultarTicket` (truncado, 3 caracteres menos que el
`ticketNumber` original de `CrearTicket`) — usar el original completo da 404 "No existe una
venta con el ID proporcionado". No documentado en ningún lado del spec — hallazgo empírico.

### 11.5 Refactor grande — Pega3 ya no tiene CREATE_TICKET

Confirmado con el usuario: a diferencia de Tradicionales (que sí reserva con
`RecuperarNumerosDisponiblesPorCombinacion` antes de vender con `VentaBoletos`), Pega3 no
tiene ningún paso de reserva — `CrearTicket` vende el ticket completo en una sola llamada
(`"status":"Purchased"`). El diseño original (`PRECHECK → CREATE_TICKET → EXECUTE → VERIFY →
REVERSE`, igual a Tradicionales) asumía un modelo de 2 fases que no existe para este
proveedor — EXECUTE nunca llamaba a nada útil (después del fix de la sesión pasada que quitó
el uso indebido de `PagarTicket`, quedaba solo haciendo eco).

**Cambio de contrato con el POS**: Pega3 ya no expone `CREATE_TICKET`. El flujo pasa a ser
`PRECHECK → EXECUTE (CrearTicket) → VERIFY → [REVERSE]`. El POS ya no debe llamar
`/v1/createTicket` para Pega3 — todo (`ticket_data`, panels, entry_type, amount) va directo
en el body de `/v1/execute`.

**Archivos**:
- `LoteriaPega3CreateTicketStrategy.java` — **eliminada**.
- `LoteriaPega3ExecuteStrategy.java` — ahora llama `CrearTicket` directo (recibe `ticket_data`
  vía `ExecuteRequest`, nuevo campo agregado con su propio `TicketData`/`TicketPanel` anidado).
- `ExecuteResponse` — agregado `ticket_number`/`ticket_qr` (antes solo en `CreateTicketResponse`).
- `RegistroTrxPort.findCreateTicketUuidByAuthorization` → renombrado
  `findExecuteUuidByAuthorization`, SQL cambiado de `CAPABILITY='CREATE_TICKET'` a
  `CAPABILITY='EXECUTE'` — la hipótesis de `transaccion` (sección 9.5) ahora busca el `uuid`
  en el registro de EXECUTE, que es donde Pega3 registra la venta.
- **`IdentifierRequiredRule`** — bug encontrado al probar el nuevo flujo: exigía
  `phone`/`document`/`userid`/`authorization` en todo EXECUTE de CASH_IN, lo cual tenía sentido
  cuando EXECUTE solo *confirmaba* un ticket ya identificado por CREATE_TICKET — pero ahora
  Pega3 vende un ticket recién creado sin comprador identificado (mismo caso ya exento para
  `CreateTicketRequest`). ✅ Corregido: se exime cuando `ExecuteRequest.ticket_data != null`.
- **`IN_OMNI_PROVEEDOR_WS`** (script `32_UPDATE_PEGA3_EXECUTE_CREARTICKET.sql`): `EXECUTE.CASHIN`
  de pega3 reapuntado de `PagarTicket` (endpoint equivocado, usado antes de este cambio) a
  `CrearTicket`. `CREATE_TICKET.CASHIN` se **deshabilita** (`ENABLED='N'`, no se borra) — no
  alcanza con dejar de usarla en código: las capabilities de `/business-lines` se derivan
  directo de `IN_OMNI_PROVEEDOR_WS` (`ad-capabilities.sql`/`capabilities.sql`, filtran
  `ENABLED='S'`), así que si se deja habilitada el front seguiría viendo `CREATE_TICKET` como
  opción disponible para Pega3 aunque el código ya no la soporte.
- `docs/CLAUDE.md` — corregidas las referencias desactualizadas (flujo Pega3, tabla de
  persistencia entre fases, lista de capabilities por proveedor — que además tenía a
  Tradicionales sin `CREATE_TICKET`, cuando sí lo tiene desde el refactor de 2026-07-03).
- Tradicionales **no se toca** — sigue con `CREATE_TICKET` real porque sí reserva.

Verificación: `mvn compile` + `mvn test` en verde (123/123) después de cada cambio.

### 11.6 Postman v10

Actualizado: Pega3/Pega4/Pega2 CASH_IN reestructurado (CREATE_TICKET fusionado dentro de
EXECUTE, renumerado 1→2/2b/2c→3→4), y `comprobante_urls` (lista) reemplazando el campo
`comprobante_b64` — que seguía ahí desde una versión muy anterior del contrato, nunca
actualizado en ninguna sesión previa, en las 3 Tradicionales y las 3 Pega. Changelog agregado
a la descripción de "📌 NOTAS v10" del propio JSON.

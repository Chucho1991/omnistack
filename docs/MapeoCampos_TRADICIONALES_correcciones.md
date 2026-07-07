# Correcciones al mapeo de campos — LN Tradicionales (Lotería / Lotto / Pozo Millonario)

Documento complementario a `MapeoCampos_v8.xlsx`. No reemplaza el Excel — recoge las correcciones
y hallazgos de campo confirmados contra `docs/LOTERIA NACIONAL.docx` (spec técnico real del
proveedor) durante la sesión de trabajo del `feature/HU-13052026` (2026-07-05 / 2026-07-06), para
trasladarlas manualmente a las hojas `PRECHECK`, `CREATE_TICKET`, `EXECUTE`, `VERIFY` del Excel.

**Nota general:** la hoja `PRECHECK` del Excel actual describe una estructura anidada
`game_catalog[].draws[]` para Tradicionales que **no coincide** con lo implementado. La estructura
real es la de abajo (arrays de nivel superior `draws[]` / `figures[]`, sin ningún campo
`games`/`game_catalog`).

---

## PRECHECK

### Request
Sin cambios — campos estándar (`uuid`, `chain`, `store`, `store_name`, `pos`, `channel_POS`,
`category_code`, `subcategory_code`, `service_provider_code`, `rms_item_code`) + `game_id` opcional
(`PrecheckRequest.gameId`, se resuelve internamente si no se envía).

### Response — `draws[]` (array de nivel superior, YA NO anidado bajo `game_catalog`)

| Campo OmniStack | Tipo | Campo real proveedor (`RecuperarSorteosDisponibles.listaDetalle[]`) | Nota |
|---|---|---|---|
| `draw_id` | string | `sorteoId` | — |
| `nombre` | string | `nombreSorteo` | — |
| `fecha` | string | `fechaSorteo` | — |
| `precio` | decimal | `pvp` | — |
| `premio_mayor` | decimal | `valorPremio` | — |
| `cantidad_fraccion` | integer | `cantidadFraccion` | — |
| `tiene_revancha` | boolean | `tieneRevancha` | — |
| `juego_revancha_id` | string | `juegoRevanchaId` | — |
| `sorteo_revancha_id` | string | `sorteoRevanchaId` | — |
| ~~`disponible`~~ | ~~boolean~~ | **no existe en el proveedor** | 🔴 **Eliminado** — campo fabricado sin respaldo en el spec, siempre venía `null` |

### Response — `figures[]` (array de nivel superior; solo con datos reales para Pozo Millonario)

| Campo OmniStack | Tipo | Campo real proveedor (`RecuperarFigurasPorJuego.listaDetalle[]`) | Nota |
|---|---|---|---|
| `figura_id` | string | `codigoImagen` | 🟢 Corregido este session — antes sin `@JsonProperty`, quedaba `null` |
| `nombre` | string | `descripcionImagen` | 🟢 Corregido — antes `null` |
| `descripcion` | string | `abreviaturaImagen` | 🟢 Corregido — antes `null` |

### Response — `games[]`
🔴 **Eliminado por completo** (campo `games` y clase `TradicionalGame`) — decisión explícita: no se
usaba en los flujos siguientes (CREATE_TICKET/EXECUTE ya reciben el juego específico vía
`rms_item_code`). La llamada a `RecuperarJuegosPorMedio` se mantiene solo para determinar
éxito/error del PRECHECK, ya no se expone su contenido.

### Validación nueva (no es un campo, es comportamiento)
Si `RecuperarSorteosDisponibles` responde `codError:0` pero sin datos (`listaDetalle:null`), el
PRECHECK ahora lanza `IntegrationException` (HTTP 502) en vez de devolver `is_error:false` con
`draws` vacío. Para Pozo Millonario (`juegoId=5`), lo mismo aplica si `RecuperarFigurasPorJuego`
viene vacío (Pozo necesita mascotas para completar la venta).

---

## CREATE_TICKET (`RecuperarNumerosDisponiblesPorCombinacion` — "obtener y reservar")

### Request

| Campo OmniStack | Tipo | Campo real proveedor | Nota |
|---|---|---|---|
| `draw_id` | string | `sorteoId` | — |
| `combinacion` | string | `combinacion` | — |
| `sugerir` | boolean | `sugerir` | — |
| `registros` | integer | `registros` | — |
| `figura_id` | string | `combinacionFigura` | Solo acepta **un** valor (String, no array) — confirmado contra spec |
| `cantidad_fracciones` | integer | `cantidad` | 🟢 **Nuevo** — antes hardcodeado a `0`. Spec confirma "Cantidad de fracciones solicitadas" (cantidad, no slot específico) |

### Response — `available_numbers[]`

| Campo OmniStack | Tipo | Campo real proveedor (`listaDetalle[]`) | Nota |
|---|---|---|---|
| `numero` | string | `numero` | — |
| `figura` | string | `figura` | — |
| `game_id` | string | `juegoId` | 🟢 Renombrado este sesión (antes se exponía como `juego_id`, inconsistente con PRECHECK/EXECUTE) |
| `draw_id` | string | `sorteoId` | 🟢 Renombrado (antes `sorteo_id`) |
| `boleto` | string | `boleto` | Asocia Pozo Millonario (5) con su Revancha (17) |
| `fracciones` | string | `fracciones` | CSV de fracciones disponibles, ej. `"1,2,...,20"` |
| ~~`disponible`~~ | ~~boolean~~ | **no existe** | 🔴 Eliminado — fabricado, siempre `null` |
| ~~`precio`~~ | ~~decimal~~ | **no existe** | 🔴 Eliminado — el precio real vive en `draws[].precio` (por sorteo, no por número) |
| `reserva_id` | string | `numeroReserva` | 🟢 **Nuevo — fix importante**. Antes este campo del proveedor no se capturaba en absoluto (se perdía en la deserialización). Debe reenviarse tal cual en EXECUTE |

---

## EXECUTE (`VentaBoletos`)

### Request

| Campo OmniStack | Tipo | Campo real proveedor | Nota |
|---|---|---|---|
| `boleto_data.game_id` / `lista_boletos[].game_id` | string | `listaJuegos[].juegoId` | — |
| `boleto_data.draw_id` / `lista_boletos[].draw_id` | string | `listaJuegos[].listaSorteos[].sorteoId` | — |
| `boleto_data.numero` / `lista_boletos[].numero` | string | `listaJuegos[].listaSorteos[].numero` | — |
| `boleto_data.cantidad_boletos` / `lista_boletos[].cantidad_boletos` | integer | `listaJuegos[].listaSorteos[].cantidadBoletos` | — |
| `reserva_id` | string | `reservaId` | 🔴🟢 **Fix crítico** — antes se enviaba el `uuid` propio de OmniStack (valor que el proveedor nunca generó). Ahora se exige el `reserva_id` real devuelto por CREATE_TICKET (`numeroReserva` del proveedor) |
| `uuid` (interno) | string | `ordenCompra` | Correcto tal cual — es el identificador de transacción de "quien consume el servicio", no algo que genere el proveedor |
| `amount` | decimal | `totalVenta` | — |
| `document` | string | `numeroIdentificacion` | — |
| `username` | string | `nombreComprador` | — |
| `phone` | string | `numeroCelularComprador` | — |

### Response

| Campo OmniStack | Tipo | Campo real proveedor | Nota |
|---|---|---|---|
| `authorization` | string | `ventaId` | — |
| `boleto_clave` | string | `listaSUE[].listaSorteos[].ListaR[].Clave` | — |
| `boleto_qr` | string | `...ListaR[].codigoQR` | — |
| `fecha_venta` | string | `fechaVenta` | — |
| `fracciones_vendidas` | string | `...ListaR[].listaNumeroFracciones[0].numeroFraccion` | 🟢 **Nuevo** — antes no se capturaba. CSV de las fracciones específicas asignadas por el proveedor (ej. `"05,06,07,11,12"`). El **segundo** registro de ese arreglo puede traer el código de mascota en vez de otra fracción (solo Pozo Millonario) — por eso solo se toma el primero |

---

## VERIFY (`GenerarComprobanteVenta`)

### Response

| Campo OmniStack | Tipo | Antes | Ahora | Nota |
|---|---|---|---|---|
| ~~`comprobante_b64`~~ → `comprobante_url` | string | Base64 inline | URL (`GET /v1/comprobantes/{id}`) | 🔴🟢 **Cambio de contrato (breaking)**. Además, el parseo estaba mal: el proveedor envuelve el PDF en JSON (`{fileName, contentType, base64}`), y el código anterior trataba la respuesta HTTP completa como si fueran los bytes crudos del PDF — se re-codificaba el JSON completo en base64 en vez del PDF real. Corregido: se parsea el JSON, se decodifica `base64`, se guarda en disco y se expone por URL |

---

## REVERSE (`AnularVentaBoletos`)
Sin cambios — revisado contra el spec, coincide exactamente (`userName`, `token`, `clienteId`,
`medioId`, `ordenCompra`, `motivo`). `ordenCompra` reutiliza el mismo `uuid` que EXECUTE, consistente.

---

## Pendientes que quedan fuera de este documento (no son mapeo de campos, son preguntas a Lotería)
- Selección de **varias mascotas** en Pozo Millonario (`combinacionFigura` es `String` singular en el spec — no hay evidencia de que acepte múltiples valores).
- **Reverse sin respuesta ni número de autorización** — requiere proceso operativo definido por negocio, no es un tema de mapeo.

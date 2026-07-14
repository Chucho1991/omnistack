# OMNISTACK Backend

Backend API REST empresarial en Java 17 y Spring Boot para orquestar multiples lineas de negocio transaccionales mediante endpoints internos estandarizados y estrategias desacopladas por proveedor.

## Estado de esta fase

Esta primera etapa no activa persistencia en base de datos. La solucion queda compilable y operativa con:

- catalogos en memoria
- auditoria en memoria
- scheduler de recarga cada 6 horas
- precarga de tokens dinamicos al arranque
- clientes externos mockeables via REST adapter
- arquitectura lista para incorporar Oracle en una segunda etapa

## Arquitectura

Se implementa una base clean/hexagonal separada por responsabilidades:

- `controller`: endpoints REST internos
- `application`: casos de uso, DTOs, mappers, puertos y servicios de orquestacion
- `domain`: modelos funcionales y enums del negocio
- `infrastructure`: adapters de catalogo, auditoria, integracion, scheduler y healthcheck
- `config`: configuraciones transversales y OpenAPI
- `shared`: validaciones, excepciones, constantes, logging y utilitarios

## Estructura de paquetes

```text
src
├── main
│   ├── java/com/omnistack/backend
│   │   ├── application
│   │   │   ├── dto
│   │   │   ├── mapper
│   │   │   ├── port
│   │   │   └── service
│   │   ├── config
│   │   ├── controller
│   │   ├── domain
│   │   │   ├── enums
│   │   │   └── model
│   │   ├── infrastructure
│   │   │   ├── adapter
│   │   │   ├── health
│   │   │   └── scheduler
│   │   └── shared
│   └── resources
└── test
```

## Requisitos

- Java 17
- Maven 3.9+

## Configuracion

La aplicacion usa `application.properties` y perfiles:

- `dev`
- `qa`
- `prod`

Propiedades principales:

- `server.port`
- `spring.application.name`
- `springdoc.api-docs.path`
- `springdoc.swagger-ui.path`
- `app.catalog.refresh.fixed-delay-ms`
- `app.catalog.refresh.initial-delay-ms`
- `app.integrations.default-connect-timeout-ms`
- `app.integrations.default-read-timeout-ms`
- `app.integrations.tls-protocols`
- `app.integrations.mock-enabled`
- `app.cashout-quota.reservation-timeout-minutes`
- `app.cashout-quota.expiration-scheduler-rate-ms`
- `app.integration.providers.default.base-url`
- `app.integration.providers.default.technical-user`
- `app.integration.providers.ecuabet.base-url`
- `app.integration.providers.ecuabet.service-provider-code`
- `app.integration.providers.ecuabet.shop-id`
- `app.integration.providers.ecuabet.country`
- `app.integration.providers.ecuabet.token`
- `app.integration.providers.ecuabet.auth.mode`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashin.item`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashin.path`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashin.capabilities`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashin.name`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashout.item`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashout.path`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashout.capabilities`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashout.name`
- `app.integration.providers.loteria.base-url`
- `app.integration.providers.loteria.category-code`
- `app.integration.providers.loteria.subcategory-code`
- `app.integration.providers.loteria.service-provider-code`
- `app.integration.providers.loteria.canal`
- `app.integration.providers.loteria.medio-id`
- `app.integration.providers.loteria.punto-operacion-id`
- `app.integration.providers.loteria.auth.mode`
- `app.integration.providers.loteria.auth.ttl-hours`
- `app.integration.providers.loteria.auth.refresh-on-startup`
- `app.integration.providers.loteria.auth.login.path`
- `app.integration.providers.loteria.auth.login.username`
- `app.integration.providers.loteria.auth.login.password`
- `app.integration.providers.loteria.auth.login.product-to-sell`
- `app.integration.providers.loteria.services.<CAPABILITY>.cashin.item`
- `app.integration.providers.loteria.services.<CAPABILITY>.cashin.path`
- `app.integration.providers.loteria.services.<CAPABILITY>.cashin.capabilities`
- `app.integration.providers.loteria.services.<CAPABILITY>.cashin.name`
- `app.integration.providers.loteria.services.<CAPABILITY>.cashout.item`
- `app.integration.providers.loteria.services.<CAPABILITY>.cashout.path`
- `app.integration.providers.loteria.services.<CAPABILITY>.cashout.capabilities`
- `app.integration.providers.loteria.services.<CAPABILITY>.cashout.name`
- `logging.level.com.omnistack.backend`

## Ejecucion local

```bash
mvn spring-boot:run
```

Puerto por defecto de la aplicacion: `8085`.

## Docker

Archivos incluidos para contenedorizacion:

- `Dockerfile`
- `.dockerignore`
- `.env`
- `.env.example`

Construccion de imagen:

```bash
docker build -t omnistack-backend:local .
```

Ejecucion con Docker:

```bash
docker run -d --name omnistack --env-file .env -p 8085:8085 omnistack-backend:local
```

Variables de entorno principales:

- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`
- `APP_CATALOG_REFRESH_FIXED_DELAY_MS`
- `APP_CATALOG_REFRESH_INITIAL_DELAY_MS`
- `APP_INTEGRATIONS_DEFAULT_CONNECT_TIMEOUT_MS` (default `60000`)
- `APP_INTEGRATIONS_DEFAULT_READ_TIMEOUT_MS` (default `60000`)
- `APP_INTEGRATIONS_TLS_PROTOCOLS` (default `TLSv1.2`)
- `APP_INTEGRATIONS_MOCK_ENABLED`
- `APP_CASHOUT_QUOTA_RESERVATION_TIMEOUT_MINUTES` (default `30`)
- `APP_CASHOUT_QUOTA_EXPIRATION_SCHEDULER_RATE_MS` (default `60000`)
- `APP_INTEGRATION_PROVIDERS_DEFAULT_BASE_URL`
- `APP_INTEGRATION_PROVIDERS_DEFAULT_TECHNICAL_USER`
- `APP_INTEGRATION_PROVIDERS_ECUABET_BASE_URL`
- `APP_INTEGRATION_PROVIDERS_ECUABET_CATEGORY_CODE`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SUBCATEGORY_CODE`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICE_PROVIDER_CODE`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SHOP_ID`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SHOP_IP` (opcional; si esta vacio se resuelve desde la IP local del servidor)
- `APP_INTEGRATION_PROVIDERS_ECUABET_COUNTRY`
- `APP_INTEGRATION_PROVIDERS_ECUABET_TOKEN`
- `APP_INTEGRATION_PROVIDERS_ECUABET_AUTH_MODE`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_PRECHECK_CASHIN_ITEM`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_PRECHECK_CASHIN_PATH`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_PRECHECK_CASHOUT_ITEM`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_PRECHECK_CASHOUT_PATH`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_EXECUTE_CASHIN_ITEM`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_EXECUTE_CASHIN_PATH`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_EXECUTE_CASHOUT_ITEM`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_EXECUTE_CASHOUT_PATH`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_REVERSE_CASHIN_ITEM`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_REVERSE_CASHIN_PATH`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_REVERSE_CASHOUT_ITEM`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_REVERSE_CASHOUT_PATH`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_BASE_URL`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_CATEGORY_CODE`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SUBCATEGORY_CODE`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICE_PROVIDER_CODE`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_CANAL`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_MEDIO_ID`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_PUNTO_OPERACION_ID`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SHOP_IP`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_CLIENTE_ID`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_PRECHECK_CASHIN_ITEM`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_PRECHECK_CASHIN_PATH`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_PRECHECK_CASHIN_CAPABILITIES`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_PRECHECK_CASHIN_NAME`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_EXECUTE_CASHIN_ITEM`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_EXECUTE_CASHIN_PATH`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_EXECUTE_CASHIN_CAPABILITIES`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_EXECUTE_CASHIN_NAME`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_REVERSE_CASHIN_ITEM`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_REVERSE_CASHIN_PATH`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_REVERSE_CASHIN_CAPABILITIES`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_REVERSE_CASHIN_NAME`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_EXECUTE_CASHOUT_ITEM`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_EXECUTE_CASHOUT_PATH`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_EXECUTE_CASHOUT_CAPABILITIES`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_EXECUTE_CASHOUT_NAME`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_AUTH_MODE`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_AUTH_TTL_HOURS`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_AUTH_REFRESH_ON_STARTUP`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_AUTH_LOGIN_PATH`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_AUTH_LOGIN_USERNAME`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_AUTH_LOGIN_PASSWORD`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_AUTH_LOGIN_PRODUCT_TO_SELL`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_PRECHECK_CASHOUT_ITEM`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_PRECHECK_CASHOUT_PATH`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_PRECHECK_CASHOUT_CAPABILITIES`
- `APP_INTEGRATION_PROVIDERS_LOTERIA_SERVICES_PRECHECK_CASHOUT_NAME`

## Build y pruebas

```bash
mvn clean test
```

## Catalogo business-lines

El endpoint `POST /business-lines` consulta Oracle por medio de un adapter dedicado y cachea el resultado por llave de request durante 6 horas. Adicionalmente, el catálogo base del backend se refresca cada 6 horas desde el mismo adapter usando un request por defecto configurable.

- Conexion Oracle configurada en `app.business-lines.oracle.datasource1.*`
- Cache de 6 horas configurable en `app.business-lines.cache.ttl-hours`
- Longitud maxima por linea de `consent_text` configurable en `app.business-lines.consent-text-max-line-length` (`APP_BUSINESS_LINES_CONSENT_TEXT_MAX_LINE_LENGTH`, por defecto 56)
- El placeholder `{{provider_name}}` en `consent_text` se resuelve con el `provider_name` del proveedor antes de responder.
- Request por defecto del refresco global configurable en `app.business-lines.default-request.*`
- La respuesta expone solo servicios cuyo `rms_item_code` este configurado como `item` en `app.integration.providers.*.services.*.(cashin|cashout).item`; si no existen items configurados, no se aplica este filtro.
- Fuente SQL mock inicial en [src/main/resources/sql/business-lines/oracle/category-subcategory.sql](/d:/Documentos/06%20-%20Recaudos/00.Fuente/omnistack/src/main/resources/sql/business-lines/oracle/category-subcategory.sql)
- Catalogos simulados desde `dual`: category/subcategory, service providers, services, capabilities, input fields y payment methods
- ECUABET CASH_OUT (`rms_item_code=100708846`) expone en `input_fields` solo `withdrawId`, `password` y `amount` para `PRECHECK`; los campos de reverso no forman parte del contrato de catalogo.
- BET 593 CASH_OUT (`rms_item_code=100708848`) expone `PRECHECK` y `VERIFY` para consultar retiro mediante `ConsultarRetiroBet593`; en `input_fields` expone solo `document`, `withdrawId` y `amount` para `PRECHECK`.
- Mientras no exista integracion con base de datos real, los campos `chain`, `store`, `store_name` y `pos` se aceptan y se replican en la respuesta sin condicionar el catalogo devuelto.
- Los `WHERE` del SQL mock de Oracle filtran temporalmente solo por `channel_POS`.

## Endpoints expuestos

- `POST /business-lines`
- `GET /healthcheck`
- `POST /v1/precheck`
- `POST /v1/execute`
- `POST /v1/verify`
- `POST /v1/reverse`
- `POST /v1/provider-token/refresh`
- `GET /actuator/health`
- `GET /swagger-ui.html`

## Postman

Se incluyen artefactos versionados para pruebas manuales en la carpeta `postman/`:

- [omnistack-backend.postman_collection.json](/omnistack/postman/omnistack-backend.postman_collection.json)
- [omnistack-local.postman_environment.json](/omnistack/postman/omnistack-local.postman_environment.json)

La coleccion esta organizada por carpetas de escenario para facilitar pruebas manuales por flujo:

- `Health`
- `Catalogo Comercial`
- `Escenarios / Cashin ECUABET`
- `Escenarios / Cashout ECUABET`
- `Escenarios / Cashin LOTERIA BET593`
- `Escenarios / Cashout LOTERIA BET593`
- `Operaciones Genericas`
- `Provider Tokens`

El environment local centraliza las variables comunes de ejecucion (`baseUrl`, `chain`, `store`, `storeName`, `pos`, `channelPos`, `correlationId`), los codigos de proveedor/categoria/subcategoria, los `rms_item_code`, montos y datos de prueba de cada escenario. Para personalizar una corrida manual se debe modificar el environment en lugar de editar los payloads de cada request.

## Ejemplos

### GET `/healthcheck`

```json
{
  "status": "UP",
  "application": "omnistack-backend",
  "timestamp": "2026-04-22T12:00:00Z",
  "catalogVersion": "memory-v1"
}
```

### POST `/business-lines`

```json
{
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "movement_type_filter": "CASH_IN"
}
```

### Response `/business-lines`

```json
{
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "collection_subcategory": [
    {
      "category_code": "1",
      "category_name": "ENTRETENIMIENTO",
      "subcategory_code": "1",
      "subcategory_name": "APUESTAS",
      "is_active": true,
      "service_providers": [
        {
          "service_provider_code": "1",
          "ruc_provider": "9999999999001",
          "provider_name": "ECUABET",
          "is_active": true,
          "services": [
            {
              "rms_item_code": "100713841",
              "description": "ECUABET CASH IN",
              "is_active": true,
              "jde_code": "ABC1234",
              "movement_type": "CASH_IN",
              "is_mixed_payment": true,
              "flg_item": "RECA",
              "is_refund": true,
              "min_amount": "1",
              "max_amount": "200",
              "timeout_ws_max": "10000",
              "retries_ws_max": "3",
              "num_tickets": "3",
              "capabilities": [
                "PRECHECK",
                "EXECUTE",
                "REVERSE"
              ],
              "input_fields": [
                {
                  "id": "document",
                  "label": "Documento Usuario",
                  "type": "STRING",
                  "capability": "PRECHECK",
                  "required": false,
                  "group": "IDENTIFICATION",
                  "conditional": "OR"
                },
                {
                  "id": "userid",
                  "label": "ID Usuario",
                  "type": "STRING",
                  "capability": "PRECHECK",
                  "required": false,
                  "group": "IDENTIFICATION",
                  "conditional": "OR"
                }
              ],
              "payment_methods": [
                {
                  "service_payment_method_id": 1,
                  "payment_method_code": "EFECTIVO",
                  "is_active": true
                },
                {
                  "service_payment_method_id": 2,
                  "payment_method_code": "TARJETA CREDITO",
                  "is_active": true
                }
              ],
              "requires_consent": true,
              "consent_text": "Autorizo de forma expresa la creación de mi registro y el uso de mis datos personales para acceder a los servicios digitales de ECUABET, incluyendo la validación de mi identidad, la gestión de apuestas y el procesamiento de pagos. Declaro que acepto los términos y condiciones del servicio y la política de tratamiento de datos personales, reconociendo mi responsabilidad en el uso de este servicio."
            }
          ]
        }
      ]
    }
  ]
}
```

### POST `/v1/precheck`

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "2",
  "rms_item_code": "100708850",
  "document": "0901111112",
  "amount": 9.99
}
```

### Response `/v1/precheck`

```json
{
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "1",
  "rms_item_code": "100713841",
  "is_error": false,
  "status": {
    "code": "00",
    "message": "Transacción correcta"
  },
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "username": "Carlos",
  "lastname": "Perez",
  "currency": "USD",
  "authorization": "AUTO-1234567890ABCDEF",
  "serialnumber": "SN-001",
  "userid": "997561",
  "document": "0912345678",
  "amount": 25.50
}
```

### POST `/v1/execute` ECUABET CASH_IN

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA EL BATAN",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "1",
  "rms_item_code": "100713841",
  "userid": "997561",
  "phone": "123456",
  "document": "0912345678",
  "amount": 100000.00
}
```

### Response `/v1/execute` ECUABET CASH_IN

```json
{
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA EL BATAN",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "1",
  "rms_item_code": "100713841",
  "is_error": false,
  "status": {
    "code": "0",
    "message": "Transaccion correcta"
  },
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "username": "Carlos",
  "lastname": "Perez",
  "currency": "USD",
  "authorization": "91081",
  "document": "0912345678",
  "amount": 100000.00
}
```

### POST `/v1/provider-token/refresh`

```json
{
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "2"
}
```

### Response `/v1/provider-token/refresh`

```json
{
  "is_error": false,
  "status": {
    "code": "00",
    "message": "Token actualizado correctamente"
  },
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "2",
  "provider_name": "LOTERIA NACIONAL",
  "refreshed_at": "2026-04-24T11:00:00-05:00",
  "expires_at": "2026-04-25T11:00:00-05:00"
}
```

## Recarga de catalogos

- Se ejecuta una carga inicial al arrancar la aplicacion.
- El scheduler refresca el snapshot segun `app.catalog.refresh.fixed-delay-ms`.
- Si una recarga falla, se conserva la ultima version valida en memoria.

## Gestion de tokens de proveedor

- El backend resuelve tokens por `category_code + subcategory_code + service_provider_code`.
- `ProviderTokenService` soporta dos modos:
- `STATIC`: usa el valor configurado en `app.integration.providers.<provider>.token`.
- `LOGIN`: invoca un endpoint de autenticacion, cachea el token en memoria y controla expiracion por `app.integration.providers.<provider>.auth.ttl-hours`.
- Un mismo `service_provider_code` puede tener multiples mecanismos de token; la resolucion toma la configuracion mas especifica para la categoria/subcategoria solicitada.
- Los proveedores con `auth.refresh-on-startup=true` se refrescan automaticamente al iniciar la aplicacion.
- Cuando un token dinamico expira, se regenera automaticamente en la siguiente solicitud que lo necesite.
- En consumos LOTERIA, si el proveedor responde un error de negocio asociado a token invalido, vencido, caducado o no autorizado, OMNISTACK fuerza la regeneracion del token y reintenta una sola vez la misma operacion con el nuevo token.
- `POST /v1/provider-token/refresh` fuerza el refresh manual del proveedor solicitado.
- El endpoint manual solo aplica a proveedores con `auth.mode=LOGIN`; si el proveedor usa token estatico responde error de negocio.

## Homologacion de codigo de autorizacion

Cuando un item tiene `AD_SERVICIO_PARAMETROS.ID_HOMOLOGADO = 'S'`, OmniStack no expone el codigo de autorizacion original del proveedor al POS. En su lugar:

- **EXECUTE / CREATE_TICKET**: genera un codigo alfanumerico unico de 10 caracteres y lo devuelve en el campo `authorization` del response. El codigo original del proveedor queda en `IN_OMNI_REGISTRO_TRX.AUTHORIZATION` y el homologado en `CP_VAR1`.
- **REVERSE**: el POS envia el codigo homologado en `authorization`. OmniStack resuelve el codigo original del proveedor desde BD y lo envia al endpoint externo.

Script de migracion: `docs/bdd/omnistack/25_ALTER_AD_SERVICIO_PARAMETROS_ID_HOMOLOGADO.sql`

## Control de cupos diarios CASH_OUT

OmniStack implementa un control de cupos maximos diarios de retiro por local (farmacia) para transacciones CASH_OUT. El cupo se administra a nivel de item RMS por farmacia.

### Reglas de negocio

- El campo `MONTO_MAX` de `AD_SERVICIO_PARAMETROS` define simultaneamente:
  - El monto maximo por transaccion individual
  - El cupo maximo diario por local para ese item CASH_OUT
- Si una transaccion alcanza el cupo maximo diario, el local no puede emitir un nuevo CASH_OUT hasta el dia siguiente.

### Flujo del cupo

1. **PRECHECK**: Reserva el monto como cupo pendiente (estado `RESERVADO`). Si el cupo disponible es insuficiente, responde `is_error=true` con codigo `BUSINESS_ERROR` (HTTP 422).
2. **EXECUTE**: Confirma la reserva (estado `CONFIRMADO`). El cupo queda consumido definitivamente.
3. **REVERSE** (mismo dia): Restituye el cupo al saldo disponible del local (estado `REVERTIDO`). Si el reverso se realiza en una fecha posterior a la transaccion original, el cupo NO se restablece.
4. **Expiracion automatica**: Un scheduler periodico expira reservas no confirmadas tras el timeout configurable (estado `EXPIRADO`), restituyendo el cupo.

### Tabla de bitacora

Esquema: `TUKUNAFUNC`  
Tabla: `IN_OMNI_CASHOUT_CUPO_DIARIO`  
Script DDL: `docs/bdd/omnistack/26_DDL_CASHOUT_CUPO_DIARIO.sql`

Estados: `RESERVADO` | `CONFIRMADO` | `EXPIRADO` | `REVERTIDO`

### Configuracion

| Property | Env var | Default | Descripcion |
|---|---|---|---|
| `app.cashout-quota.reservation-timeout-minutes` | `APP_CASHOUT_QUOTA_RESERVATION_TIMEOUT_MINUTES` | `30` | Minutos para confirmar una reserva antes de expirarla |
| `app.cashout-quota.expiration-scheduler-rate-ms` | `APP_CASHOUT_QUOTA_EXPIRATION_SCHEDULER_RATE_MS` | `60000` | Intervalo del scheduler de expiracion (ms) |

### Clases involucradas

- `CashOutQuotaService` — logica de negocio (reservar, confirmar, revertir, expirar)
- `CashOutQuotaPort` — puerto de salida
- `OracleCashOutQuotaAdapter` — adaptador Oracle
- `CashOutQuotaExpirationScheduler` — scheduler periodico
- `CashOutQuotaEntry` — modelo de dominio
- `CashOutQuotaStatus` — enum de estados

## Integraciones externas

La resolucion de flujos depende de:

- `ProviderFlowResolver`
- `TransactionFlowStrategy`
- `ExternalProviderClient`
- `PrecheckStrategy`
- `ExecuteStrategy`
- `VerifyStrategy`
- `ReverseStrategy`

No hay logica por proveedor en los controllers. Las integraciones externas quedan reales por defecto. Si un servicio catalogado no tiene estrategia y endpoint externo configurados, OMNISTACK responde error de configuracion en lugar de simular una respuesta exitosa.

### Validacion de monto maximo/minimo por transaccion

Para todos los servicios que NO son CASH_OUT, el orquestador valida que el monto de cada transaccion este dentro del rango [`MONTO_MIN`, `MONTO_MAX`] configurado en `AD_SERVICIO_PARAMETROS` para el item correspondiente.

- Se ejecuta en PRECHECK y EXECUTE antes de invocar al proveedor externo.
- Si el monto excede MONTO_MAX o es inferior a MONTO_MIN, responde HTTP 422 con error de negocio descriptivo.
- Este control es por transaccion individual — no involucra cupos diarios ni acumulados.
- Clase responsable: `TransactionAmountValidationService`
- Los items CASH_OUT quedan excluidos porque ya tienen validacion de monto dentro de `CashOutQuotaService`.

### ECUABET Buscar usuario

La integracion inicial de ECUABET para `PRECHECK` usa el endpoint externo `POST /user/search` con:

- headers: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body: `shop`, `token`, `userid`, `country`, `phone`, `document`
- identidad del proveedor: `service_provider_code`
- `category_code` y `subcategory_code` siguen viajando en el contrato, pero no definen el proveedor; un mismo `service_provider_code` puede existir en varias subcategorias
- response interna: replica `chain`, `store`, `store_name`, `pos`, `channel_POS`, `uuid`, `category_code`, `subcategory_code`, `service_provider_code` y `rms_item_code`
- mapeo funcional: `is_error <- error != 0 o code distinto de 0/00`, `error.code <- 02 para usuario invalido/no encontrado, 01 para otros errores, 00 para exito`, `error.message <- message/error`, `username <- name`, `status.code <- 00`, `status.message <- "Transacción correcta"`
- `authorization`: si ECUABET no la retorna, OMNISTACK la genera automaticamente
- `movement_type` no es requerido en los endpoints transaccionales; la aplicacion resuelve CASH_IN o CASH_OUT desde la definicion del servicio en catalogo/business-lines segun `rms_item_code`
- resolucion de ruta: OMNISTACK usa `provider -> capability -> flow(cashin/cashout)`, validando el `item` configurado contra el `rms_item_code` del servicio

El adapter HTTP real invoca `https://apidev.virtualsoft.tech/operatorapi-new/user/search` o la URL configurada por propiedades.

ECUABET queda configurado con `auth.mode=STATIC`, por lo que el token se resuelve desde propiedades a traves del modulo generico de tokens.

### ECUABET PRECHECK CASH_OUT

Este bloque complementa la descripcion anterior con el flujo de Nota de Retiro para `service_provider_code=12661912` y `rms_item_code=100708846`.

- endpoint externo: `POST /user/searchwithdraw`
- headers comunes: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body externo: `shop`, `token`, `withdrawId`, `country`, `password`
- mapeo de response: `is_error <- error != 0 o code distinto de 0/00`, `error.code <- codigo canonico derivado de code/message/error`, `error.message <- message/error`, `username <- name`, `currency <- currency`, `amount <- amount`, `userid <- userId|userid`
- validacion transversal de monto: si cualquier integracion retorna `amount` y este es mayor o menor que el `amount` del request interno, OMNISTACK responde `is_error=true`, `error.code=01` y `error.message` describe si el monto solicitado es mayor o menor que el monto retornado por el proveedor externo
- `authorization`: si ECUABET no la retorna, OMNISTACK la genera automaticamente

Ejemplo `PRECHECK CASH_OUT`:

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "1",
  "rms_item_code": "100708846",
  "userid": "",
  "phone": "",
  "withdrawId": "7667",
  "password": "88422",
  "document": "",
  "amount": 1.00
}
```

### Response de error

```json
{
  "is_error": true,
  "error": {
    "code": "01",
    "message": "La solicitud no cumple las validaciones requeridas"
  }
}
```

Codigos estandar de respuesta canonica:

- `00`: OK - Operacion exitosa.
- `01`: Error.
- `02`: Usuario invalido.

Los codigos tecnicos de proveedor (`code`, `codError`, HTTP o similares) no se exponen como `error.code`; se traducen al canonico `00`, `01` o `02` y el detalle tecnico/funcional queda en `error.message`.

El adapter HTTP real invoca `https://apidev.virtualsoft.tech/operatorapi-new/user/searchwithdraw` cuando el servicio resuelto corresponde a `CASH_OUT`.

### ECUABET EXECUTE CASH_IN

La recarga de saldos ECUABET usa `service_provider_code=12661912` y `rms_item_code=100713841` para ejecutar el deposito externo.

- endpoint externo: `POST /user/deposit`
- headers comunes: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body externo: `shop`, `token`, `userid`, `country`, `amount`, `transactionId`, `shop_info`, `shop_ip`
- `transactionId`: OMNISTACK genera un entero para enviarlo a ECUABET; si ECUABET retorna `transactionId`, se devuelve al consumidor como `authorization`
- `shop_info`: se mapea desde `store_name`
- `shop_ip`: usa `APP_INTEGRATION_PROVIDERS_ECUABET_SHOP_IP` si esta configurado; si no, se resuelve desde la IP local del servidor
- mapeo response: `is_error <- error`, `error.code <- codigo canonico derivado de code/error/message`, `error.message <- error/message`, `username <- nombre|name`, `lastname <- apellido|lastname`, `currency <- currency`, `status.code <- 00`, `status.message <- "Transaccion correcta"`, `authorization <- transactionId externo`, `document <- document`, `amount <- amount`
- seguridad: el endpoint interno conserva el mecanismo actual del backend; la autorizacion por rol queda como pendiente tecnico mientras no exista un modulo de seguridad configurado en el proyecto

Request externo generado:

```json
{
  "shop": "998739",
  "token": "token-ecuabet",
  "userid": 997561,
  "country": 66,
  "amount": 100000.00,
  "transactionId": 91081,
  "shop_info": "FYBECA EL BATAN",
  "shop_ip": "10.0.0.10"
}
```

### ECUABET REVERSE CASH_IN

El reverso de recarga ECUABET usa `service_provider_code=12661912` y `rms_item_code=100713841` para invocar el rollback externo de deposito.

- endpoint externo: `POST /rollback/deposit`
- headers comunes: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body externo: `shop`, `token`, `country`, `amount`, `transactionId`
- `transactionId`: se mapea desde `authorization` del request interno y debe ser numerico
- `authorization`: en la respuesta interna se conserva el `transactionId` enviado al request externo; el `transactionId` retornado por ECUABET se registra como dato de proveedor y no reemplaza la autorizacion del flujo
- mapeo response: `is_error <- error`, `error.code <- codigo canonico derivado de code/error/message`, `error.message <- error/message`, `username <- nombre|name`, `lastname <- apellido|lastname`, `currency <- currency`, `status.code <- 00`, `status.message <- "Transaccion correcta"`, `authorization <- authorization interno`, `document <- document`, `amount <- amount`
- seguridad: el endpoint interno conserva el mecanismo actual del backend; la autorizacion por rol queda como pendiente tecnico mientras no exista un modulo de seguridad configurado en el proyecto

Request interno:

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA EL BATAN",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "1",
  "rms_item_code": "100713841",
  "authorization": "91081",
  "document": "0912345678",
  "amount": 100000.00,
  "motivo": "Reverso por timeout del proveedor"
}
```

Request externo generado:

```json
{
  "shop": "998739",
  "token": "token-ecuabet",
  "country": 66,
  "amount": 100000.00,
  "transactionId": 91081
}
```

### ECUABET EXECUTE CASH_OUT

La ejecucion de nota de retiro ECUABET usa `service_provider_code=12661912` y el `rms_item_code` CASH_OUT expuesto por business-lines (`100708846` en el catalogo actual).

- endpoint externo: `POST /user/withdraw`
- headers comunes: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body externo: `shop`, `token`, `withdrawId`, `country`, `password`, `transactionId`, `shop_info`, `shop_ip`
- `transactionId`: OMNISTACK genera un entero para enviarlo a ECUABET; si ECUABET retorna `transactionId`, se devuelve al consumidor como `authorization`
- `shop_info`: se mapea desde `store_name`
- `shop_ip`: usa `APP_INTEGRATION_PROVIDERS_ECUABET_SHOP_IP` si esta configurado; si no, se resuelve desde la IP local del servidor
- mapeo response: `is_error <- error`, `error.code <- codigo canonico derivado de code/error/message`, `error.message <- error/message`, `status.code <- 00`, `status.message <- "Transaccion correcta"`, `authorization <- transactionId externo`, `document <- document`, `amount <- amount`
- seguridad: el endpoint interno conserva el mecanismo actual del backend; la autorizacion por rol queda como pendiente tecnico mientras no exista un modulo de seguridad configurado en el proyecto

Request interno:

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA EL BATAN",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "1",
  "rms_item_code": "100708846",
  "withdrawId": "7668",
  "password": "77992",
  "document": "0912345678",
  "amount": 25.50
}
```

Request externo generado:

```json
{
  "shop": "998739",
  "token": "token-ecuabet",
  "withdrawId": "7668",
  "country": 66,
  "password": "77992",
  "transactionId": 10980,
  "shop_info": "FYBECA EL BATAN",
  "shop_ip": "10.0.0.10"
}
```

### ECUABET REVERSE CASH_OUT

El reverso de nota de retiro ECUABET usa `service_provider_code=12661912` y el `rms_item_code` CASH_OUT expuesto por business-lines (`100708846` en el catalogo actual).

- endpoint externo: `POST /rollback/withdraw`
- headers comunes: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body externo: `shop`, `token`, `country`, `withdrawId`, `password`, `transactionId`
- `transactionId`: OMNISTACK genera un entero para enviarlo a ECUABET; si ECUABET retorna `transactionId`, se devuelve al consumidor como `authorization`
- mapeo response: `is_error <- error`, `error.code <- codigo canonico derivado de code/error/message`, `error.message <- error/message`, `status.code <- 00`, `status.message <- "Transaccion correcta"`, `authorization <- transactionId externo`, `document <- document`, `amount <- amount`
- seguridad: el endpoint interno conserva el mecanismo actual del backend; la autorizacion por rol queda como pendiente tecnico mientras no exista un modulo de seguridad configurado en el proyecto

Request interno:

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA EL BATAN",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "1",
  "rms_item_code": "100708846",
  "withdrawId": "7671",
  "password": "03448",
  "document": "0912345678",
  "amount": 25.50,
  "motivo": "Reverso de nota de retiro"
}
```

Request externo generado:

```json
{
  "shop": "998739",
  "token": "token-ecuabet",
  "country": 66,
  "withdrawId": "7671",
  "password": "03448",
  "transactionId": 10980
}
```

### LOTERIA BET593 PRECHECK CASH_IN

La recarga de saldos BET593 usa el proveedor Loteria Nacional con resolucion por catalogo `category_code=983`, `subcategory_code=1120`, `service_provider_code=408403` y `rms_item_code=100708850`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/RecargarBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario`, `canal=BMV`, `medioId=23`, `puntooperacionId=52132`
- mapeo request: `uuid -> codigotrn`, `document -> cuentaweb`, `amount -> valor`
- mapeo response: `msgError -> is_error/error.message`, `codError/msgError -> error.code canonico`, `status.code <- 00`, `nombre -> username`, `apellido -> lastname`, `recargaid -> authorization`, `serialnumber -> serialnumber`, `cuentaweb -> document`, `valor -> amount`

Request externo generado:

```json
{
  "usuario": "USRFEMSAPREP",
  "token": "token-dinamico",
  "canal": "BMV",
  "medioId": 23,
  "puntooperacionId": 52132,
  "cuentaweb": "0901111112",
  "valor": "9.99",
  "codigotrn": "f0908f64-9145-45cf-a22c-c36bca604372"
}
```

### LOTERIA BET593 EXECUTE CASH_IN

La confirmacion de recarga de saldos BET593 usa el mismo contexto comercial `category_code=983`, `subcategory_code=1120`, `service_provider_code=408403` y `rms_item_code=100708850`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/ConfirmarBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario`, `canal=BMV`, `medioId=23`, `puntooperacionId=52132`
- mapeo request: `uuid -> codigotrn`, `document -> cuentaweb`, `authorization -> recargaid`, `serialnumber -> serialnumber`, `amount -> valor`
- mapeo response: `msgError -> is_error/error.message`, `codError/msgError -> error.code canonico`, `status.code <- 00`, `nombre -> username`, `apellido -> lastname`, `recargaid -> authorization`, `serialnumber -> serialnumber`, `cuentaweb -> document`, `valor -> amount`

Request interno:

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": 983,
  "subcategory_code": 1120,
  "service_provider_code": 408403,
  "rms_item_code": "100708850",
  "authorization": "9F968187-F436-4F19-8C1F-A7A4DA07A899",
  "serialnumber": "7366ea56284a06a2a58f561b497386b80fcd3eaea858d0c511",
  "document": "0901111112",
  "amount": 9.99
}
```

Request externo generado:

```json
{
  "usuario": "USRFEMSAPREP",
  "token": "token-dinamico",
  "canal": "BMV",
  "medioId": 23,
  "puntooperacionId": 52132,
  "cuentaweb": "0901111112",
  "recargaid": "9F968187-F436-4F19-8C1F-A7A4DA07A899",
  "serialnumber": "7366ea56284a06a2a58f561b497386b80fcd3eaea858d0c511",
  "valor": "9.99",
  "codigotrn": "f0908f64-9145-45cf-a22c-c36bca604372"
}
```

### LOTERIA BET593 VERIFY CASH_IN

La validacion de recarga BET593 consulta el estado de una recarga CASH_IN con el contexto comercial `category_code=983`, `subcategory_code=1120`, `service_provider_code=408403` y `rms_item_code=100708850`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/ValidarBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario`, `canal=BMV`, `medioId=23`, `puntooperacionId=52132`
- mapeo request: `document -> cuentaweb`, `authorization -> recargaid`, `serialnumber -> serialnumber`
- mapeo response: `msgError -> is_error/error.message`, `codError/msgError -> error.code canonico`, `status.code <- 00`, `estado=COMMIT -> status.message=Transaccion ha sido ejecutada`, `recargaid -> authorization`, `serialnumber -> serialnumber`, `cuentaweb -> document`

Request interno:

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": 983,
  "subcategory_code": 1120,
  "service_provider_code": 408403,
  "rms_item_code": "100708850",
  "authorization": "9F968187-F436-4F19-8C1F-A7A4DA07A899",
  "serialnumber": "7366ea56284a06a2a58f561b497386b80fcd3eaea858d0c511",
  "document": "0901111112"
}
```

Request externo generado:

```json
{
  "usuario": "USRFEMSAPREP",
  "token": "token-dinamico",
  "canal": "BMV",
  "medioId": 23,
  "puntooperacionId": 52132,
  "cuentaweb": "0901111112",
  "recargaid": "9F968187-F436-4F19-8C1F-A7A4DA07A899",
  "serialnumber": "7366ea56284a06a2a58f561b497386b80fcd3eaea858d0c511"
}
```

### LOTERIA BET593 EXECUTE CASH_OUT

La nota de retiro BET593 usa Loteria Nacional con resolucion por catalogo `category_code=983`, `subcategory_code=1121`, `service_provider_code=408403` y `rms_item_code=100708848`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/RetirarBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario/usuarioId`, `maquina`, `operacion=RETIROOL`, `clienteId=58542`, `medioId=23`
- mapeo request: `uuid -> numeroTransaccion`, `document -> identificacion`, `withdrawId -> numeroRetiro`
- mapeo response: `msgError -> is_error/error.message`, `codError/msgError -> error.code canonico`, `status.code <- 00`, `ordenPagoId -> authorization`, `identificacion -> document`, `valor -> amount`

Request interno:

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": 983,
  "subcategory_code": 1121,
  "service_provider_code": 408403,
  "rms_item_code": "100708848",
  "document": "0911274165",
  "withdrawId": "20240430800100007",
  "amount": 17.00
}
```

Request externo generado:

```json
{
  "usuario": "USRFEMSAPREP",
  "maquina": "192.168.3.230",
  "operacion": "RETIROOL",
  "token": "token-dinamico",
  "usuarioId": "USRFEMSAPREP",
  "clienteId": 58542,
  "medioId": 23,
  "numeroTransaccion": "f0908f64-9145-45cf-a22c-c36bca604372",
  "identificacion": "0911274165",
  "numeroRetiro": "20240430800100007"
}
```

### LOTERIA BET593 PRECHECK CASH_OUT

El precheck de nota de retiro BET593 consulta la orden CASH_OUT con el mismo endpoint externo usado por `VERIFY`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/ConsultarRetiroBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario/usuarioId`, `maquina`, `operacion=CONRETIROOL`, `clienteId=58542`, `medioId=23`
- mapeo request: `uuid -> numeroTransaccion`, `document -> identificacion`, `withdrawId -> numeroRetiro`
- regla especial: `codError=400022` se interpreta como transaccion ejecutada y responde `status.code=00`, `status.message=Transaccion correcta`
- **nota:** `subcategory_code` no se valida contra `IN_OMNI_PROVEEDOR_CONFIG` — el proveedor `loteria` tiene dos subcategorias (1120 CI, 1121 CO) y solo puede almacenar una en la tabla de config. El routing por los 4 campos del catalogo ya garantiza la asignacion correcta.

Request interno:

```json
{
  "uuid": "ca9b201a-a668-45ed-876c-00affcb18580",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": 983,
  "subcategory_code": 1121,
  "service_provider_code": 408403,
  "rms_item_code": "100708848",
  "document": "0901111112",
  "withdrawId": "340468406359",
  "amount": 17.00
}
```

### LOTERIA BET593 VERIFY CASH_OUT

La validacion de nota de retiro BET593 consulta el estado de una orden CASH_OUT con el contexto comercial `category_code=983`, `subcategory_code=1121`, `service_provider_code=408403` y `rms_item_code=100708848`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/ConsultarRetiroBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario/usuarioId`, `maquina`, `operacion=CONRETIROOL`, `clienteId=58542`, `medioId=23`
- mapeo request: `uuid -> numeroTransaccion`, `document -> identificacion`, `withdrawId -> numeroRetiro`
- mapeo response: `msgError -> is_error/error.message`, `codError/msgError -> error.code canonico`, `status.code <- 00`, `identificacion -> document`
- regla especial: `codError=400022` se interpreta como transaccion ejecutada y responde `status.code=00`, `status.message=Transaccion correcta`

Request interno:

```json
{
  "uuid": "ca9b201a-a668-45ed-876c-00affcb18580",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": 983,
  "subcategory_code": 1121,
  "service_provider_code": 408403,
  "rms_item_code": "100708848",
  "document": "0901111112",
  "withdrawId": "340468406359"
}
```

Request externo generado:

```json
{
  "usuario": "USRFEMSAPREP",
  "maquina": "192.168.3.230",
  "operacion": "CONRETIROOL",
  "token": "token-dinamico",
  "usuarioId": "USRFEMSAPREP",
  "medioId": 23,
  "clienteId": 58542,
  "numeroTransaccion": "ca9b201a-a668-45ed-876c-00affcb18580",
  "identificacion": "0901111112",
  "numeroRetiro": "340468406359"
}
```

### LOTERIA BET593 REVERSE CASH_IN

El reverso de recarga BET593 usa Loteria Nacional con el contexto comercial `category_code=759`, `subcategory_code=161`, `service_provider_code=408403` y `rms_item_code=100708850`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/ReversarRetiroBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario/usuarioId=USRFEMSAPREP`, `maquina=192.168.3.230`, `operacion=REVRETIROOL`, `clienteId=58542`, `medioId=23`
- mapeo request: `uuid -> numeroTransaccion`, `document -> identificacion`, `motivo -> motivo`
- mapeo response: `msgError -> is_error/error.message`, `codError/msgError -> error.code canonico`, `status.code <- 00`, `cuentaweb -> document`, `recargaid -> authorization`
- respuesta exitosa interna: `status.message="Transacción correcta"`

Request interno:

```json
{
  "uuid": "ca9b201a-a668-45ed-876c-00affcb18580",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "2",
  "rms_item_code": "100708850",
  "document": "0901111112",
  "motivo": "Demora en obtener respuesta"
}
```

Request externo generado:

```json
{
  "usuario": "USRFEMSAPREP",
  "maquina": "192.168.3.230",
  "operacion": "REVRETIROOL",
  "token": "token-dinamico",
  "usuarioId": "USRFEMSAPREP",
  "medioId": 23,
  "clienteId": 58542,
  "numeroTransaccion": "ca9b201a-a668-45ed-876c-00affcb18580",
  "identificacion": "0901111112",
  "motivo": "Demora en obtener respuesta"
}
```

### LOTERIA BET593 REVERSE CASH_OUT

El reverso de nota de retiro BET593 usa Loteria Nacional con el contexto comercial `category_code=759`, `subcategory_code=161`, `service_provider_code=408403` y `rms_item_code=100708848`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/ReversarRetiroBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario/usuarioId`, `maquina`, `operacion=REVRETIROOL`, `clienteId=58542`, `medioId=23`
- mapeo request: `authorization -> numeroTransaccion`, `document -> identificacion`, `motivo -> motivo`
- mapeo response: `msgError -> is_error/error.message`, `codError/msgError -> error.code canonico`, `status.code <- 00`, `identificacion -> document`, `numeroTransaccion -> authorization`

Request interno:

```json
{
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "chain": "1",
  "store": "148",
  "store_name": "FYBECA AMAZONAS",
  "pos": "1",
  "channel_POS": "POS",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "2",
  "rms_item_code": "100708848",
  "authorization": "ca9b201a-a668-45ed-876c-00affcb18580",
  "document": "0901111112",
  "motivo": "Demora en obtener respuesta"
}
```

Request externo generado:

```json
{
  "usuario": "USRFEMSAPREP",
  "maquina": "192.168.3.230",
  "operacion": "REVRETIROOL",
  "token": "token-dinamico",
  "usuarioId": "USRFEMSAPREP",
  "medioId": 23,
  "clienteId": 58542,
  "numeroTransaccion": "ca9b201a-a668-45ed-876c-00affcb18580",
  "identificacion": "0901111112",
  "motivo": "Demora en obtener respuesta"
}
```

### LOTERIA Login BET593

La autenticacion para las integraciones de Loteria se desacopla en un adapter HTTP dedicado y se administra por contexto comercial.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/Login`
- body externo: `username`, `password`, `productoVender`
- response externa: `usuario`, `token`, `codError`, `msgError`
- llave de resolucion interna: `category_code + subcategory_code + service_provider_code`
- TTL del token: configurable por `app.integration.providers.loteria.auth.ttl-hours`
- precarga inicial: controlada por `app.integration.providers.loteria.auth.refresh-on-startup`
- refresh manual interno: `POST /v1/provider-token/refresh` con `category_code`, `subcategory_code` y `service_provider_code`

Ejemplo de login configurado para BET593:

```json
{
  "username": "USRFEMSAPREP",
  "password": "F3m993sA.",
  "productoVender": "Bet593"
}
```

## OpenAPI

Swagger queda disponible en:

- `/swagger-ui.html`
- `/api-docs`

## Segunda etapa Oracle

En la siguiente fase se incorporaran:

- multiples datasources Oracle
- JPA/Hibernate
- entidades tecnicas de catalogo y auditoria
- repositorios reales
- logs transaccionales persistidos
- catalogos precargados desde esquema de parametrizacion

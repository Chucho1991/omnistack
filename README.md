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
- `app.integrations.mock-enabled`
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
- `logging.level.com.omnistack.backend`

## Ejecucion local

```bash
mvn spring-boot:run
```

Puerto por defecto de la aplicacion: `8185`.

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
docker run --name omnistack --env-file .env -p 8185:8185 omnistack-backend:local
```

Variables de entorno principales:

- `SPRING_PROFILES_ACTIVE`
- `SERVER_PORT`
- `APP_CATALOG_REFRESH_FIXED_DELAY_MS`
- `APP_CATALOG_REFRESH_INITIAL_DELAY_MS`
- `APP_INTEGRATIONS_DEFAULT_CONNECT_TIMEOUT_MS` (default `60000`)
- `APP_INTEGRATIONS_DEFAULT_READ_TIMEOUT_MS` (default `60000`)
- `APP_INTEGRATIONS_MOCK_ENABLED`
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

## Build y pruebas

```bash
mvn clean test
```

## Catalogo business-lines

El endpoint `POST /business-lines` consulta Oracle por medio de un adapter dedicado y cachea el resultado por llave de request durante 6 horas. Adicionalmente, el catálogo base del backend se refresca cada 6 horas desde el mismo adapter usando un request por defecto configurable.

- Conexion Oracle configurada en `app.business-lines.oracle.datasource1.*`
- Cache de 6 horas configurable en `app.business-lines.cache.ttl-hours`
- Request por defecto del refresco global configurable en `app.business-lines.default-request.*`
- Fuente SQL mock inicial en [src/main/resources/sql/business-lines/oracle/category-subcategory.sql](/d:/Documentos/06%20-%20Recaudos/00.Fuente/omnistack/src/main/resources/sql/business-lines/oracle/category-subcategory.sql)
- Catalogos simulados desde `dual`: category/subcategory, service providers, services, capabilities, input fields y payment methods
- Los `WHERE` de Oracle usan `chain`, `store`, `store_name`, `pos` y `channel_POS`

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
          "provider_name": "ECUABET",
          "is_active": true,
          "services": [
            {
              "rms_item_code": "10001565826",
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
              "consent_text": "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><title>Lorem Ipsum</title></head><body><h1>Lorem Ipsum</h1><p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p></body></html>"
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
  "rms_item_code": "10001565828",
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
  "rms_item_code": "10001565826",
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
  "rms_item_code": "10001565826",
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
  "rms_item_code": "10001565826",
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
- `POST /v1/provider-token/refresh` fuerza el refresh manual del proveedor solicitado.
- El endpoint manual solo aplica a proveedores con `auth.mode=LOGIN`; si el proveedor usa token estatico responde error de negocio.

## Integraciones externas

La resolucion de flujos depende de:

- `ProviderFlowResolver`
- `TransactionFlowStrategy`
- `ExternalProviderClient`
- `PrecheckStrategy`
- `ExecuteStrategy`
- `VerifyStrategy`
- `ReverseStrategy`

No hay logica por proveedor en los controllers. Las integraciones externas quedan reales por defecto. `app.integrations.mock-enabled=true` solo activa el flujo mock generico para pruebas controladas.

### ECUABET Buscar usuario

La integracion inicial de ECUABET para `PRECHECK` usa el endpoint externo `POST /user/search` con:

- headers: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body: `shop`, `token`, `userid`, `country`, `phone`, `document`
- identidad del proveedor: `service_provider_code`
- `category_code` y `subcategory_code` siguen viajando en el contrato, pero no definen el proveedor; un mismo `service_provider_code` puede existir en varias subcategorias
- response interna: replica `chain`, `store`, `store_name`, `pos`, `channel_POS`, `uuid`, `category_code`, `subcategory_code`, `service_provider_code` y `rms_item_code`
- mapeo funcional: `is_error <- error`, `error.code <- code`, `error.message <- error`, `username <- name`, `status.code <- code`, `status.message <- "Transacción correcta"`
- `authorization`: si ECUABET no la retorna, OMNISTACK la genera automaticamente
- `movement_type` no es requerido en los endpoints transaccionales; la aplicacion resuelve CASH_IN o CASH_OUT desde la definicion del servicio en catalogo/business-lines segun `rms_item_code`
- resolucion de ruta: OMNISTACK usa `provider -> capability -> flow(cashin/cashout)`, validando el `item` configurado contra el `rms_item_code` del servicio

El adapter HTTP real invoca `https://apidev.virtualsoft.tech/operatorapi-new/user/search` o la URL configurada por propiedades.

ECUABET queda configurado con `auth.mode=STATIC`, por lo que el token se resuelve desde propiedades a traves del modulo generico de tokens.

### ECUABET PRECHECK CASH_OUT

Este bloque complementa la descripcion anterior con el flujo de Nota de Retiro para `service_provider_code=1` y `rms_item_code=10001565827`.

- endpoint externo: `POST /user/searchwithdraw`
- headers comunes: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body externo: `shop`, `token`, `withdrawId`, `country`, `password`
- mapeo de response: `is_error <- error`, `error.code <- code`, `error.message <- error`, `username <- name`, `currency <- currency`, `amount <- amount`, `userid <- userId|userid`
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
  "rms_item_code": "10001565827",
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
  "code": "VAL-001",
  "message": "La solicitud no cumple las validaciones requeridas"
}
```

El adapter HTTP real invoca `https://apidev.virtualsoft.tech/operatorapi-new/user/searchwithdraw` cuando el servicio resuelto corresponde a `CASH_OUT`.

### ECUABET EXECUTE CASH_IN

La recarga de saldos ECUABET usa `service_provider_code=1` y `rms_item_code=10001565826` para ejecutar el deposito externo.

- endpoint externo: `POST /user/deposit`
- headers comunes: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body externo: `shop`, `token`, `userid`, `country`, `amount`, `transactionId`, `shop_info`, `shop_ip`
- `transactionId`: OMNISTACK genera un entero para enviarlo a ECUABET; si ECUABET retorna `transactionId`, se devuelve al consumidor como `authorization`
- `shop_info`: se mapea desde `store_name`
- `shop_ip`: usa `APP_INTEGRATION_PROVIDERS_ECUABET_SHOP_IP` si esta configurado; si no, se resuelve desde la IP local del servidor
- mapeo response: `is_error <- error`, `error.code <- code`, `error.message <- error/message`, `username <- nombre|name`, `lastname <- apellido|lastname`, `currency <- currency`, `status.code <- code`, `status.message <- "Transaccion correcta"`, `authorization <- transactionId externo`, `document <- document`, `amount <- amount`
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

### ECUABET EXECUTE CASH_OUT

La ejecucion de nota de retiro ECUABET usa `service_provider_code=1` y el `rms_item_code` CASH_OUT expuesto por business-lines (`10001565827` en el catalogo actual).

- endpoint externo: `POST /user/withdraw`
- headers comunes: `chain`, `store`, `store_name`, `pos`, `channel_POS`
- body externo: `shop`, `token`, `withdrawId`, `country`, `password`, `transactionId`, `shop_info`, `shop_ip`
- `transactionId`: OMNISTACK genera un entero para enviarlo a ECUABET; si ECUABET retorna `transactionId`, se devuelve al consumidor como `authorization`
- `shop_info`: se mapea desde `store_name`
- `shop_ip`: usa `APP_INTEGRATION_PROVIDERS_ECUABET_SHOP_IP` si esta configurado; si no, se resuelve desde la IP local del servidor
- mapeo response: `is_error <- error`, `error.code <- code`, `error.message <- error/message`, `status.code <- code`, `status.message <- "Transaccion correcta"`, `authorization <- transactionId externo`, `document <- document`, `amount <- amount`
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
  "rms_item_code": "10001565827",
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

### LOTERIA BET593 PRECHECK CASH_IN

La recarga de saldos BET593 usa el proveedor Loteria Nacional con resolucion por catalogo `category_code=1`, `subcategory_code=1`, `service_provider_code=2` y `rms_item_code=10001565828`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/RecargarBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario`, `canal=BMV`, `medioId=23`, `puntooperacionId=52132`
- mapeo request: `uuid -> codigotrn`, `document -> cuentaweb`, `amount -> valor`
- mapeo response: `msgError -> is_error/error.message`, `codError -> error.code/status.code`, `nombre -> username`, `apellido -> lastname`, `recargaid -> authorization`, `serialnumber -> serialnumber`, `cuentaweb -> document`, `valor -> amount`

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

La confirmacion de recarga de saldos BET593 usa el mismo contexto comercial `category_code=1`, `subcategory_code=1`, `service_provider_code=2` y `rms_item_code=10001565828`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/ConfirmarBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario`, `canal=BMV`, `medioId=23`, `puntooperacionId=52132`
- mapeo request: `uuid -> codigotrn`, `document -> cuentaweb`, `authorization -> recargaid`, `serialnumber -> serialnumber`, `amount -> valor`
- mapeo response: `msgError -> is_error/error.message`, `codError -> error.code/status.code`, `nombre -> username`, `apellido -> lastname`, `recargaid -> authorization`, `serialnumber -> serialnumber`, `cuentaweb -> document`, `valor -> amount`

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
  "rms_item_code": "10001565828",
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

La validacion de recarga BET593 consulta el estado de una recarga CASH_IN con el contexto comercial `category_code=1`, `subcategory_code=1`, `service_provider_code=2` y `rms_item_code=10001565828`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/ValidarBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario`, `canal=BMV`, `medioId=23`, `puntooperacionId=52132`
- mapeo request: `document -> cuentaweb`, `authorization -> recargaid`, `serialnumber -> serialnumber`
- mapeo response: `msgError -> is_error/error.message`, `codError -> error.code/status.code`, `estado=COMMIT -> status.message=Transaccion ha sido ejecutada`, `recargaid -> authorization`, `serialnumber -> serialnumber`, `cuentaweb -> document`

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
  "rms_item_code": "10001565828",
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

La nota de retiro BET593 usa Loteria Nacional con resolucion por catalogo `category_code=1`, `subcategory_code=1`, `service_provider_code=2` y `rms_item_code=10001565829`.

- endpoint externo: `POST /APIVentasLoteria/api/Ventas/RetirarBet593`
- token externo: resuelto por el modulo de tokens mediante `category_code + subcategory_code + service_provider_code`
- constantes configurables: `usuario/usuarioId`, `maquina`, `operacion=RETIROOL`, `clienteId=58542`, `medioId=23`
- mapeo request: `uuid -> numeroTransaccion`, `document -> identificacion`, `withdrawId -> numeroRetiro`
- mapeo response: `msgError -> is_error/error.message`, `codError -> error.code/status.code`, `ordenPagoId -> authorization`, `identificacion -> document`, `valor -> amount`

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
  "rms_item_code": "10001565829",
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

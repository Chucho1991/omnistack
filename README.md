# OMNISTACK Backend

Backend API REST empresarial en Java 17 y Spring Boot para orquestar multiples lineas de negocio transaccionales mediante endpoints internos estandarizados y estrategias desacopladas por proveedor.

## Estado de esta fase

Esta primera etapa no activa persistencia en base de datos. La solucion queda compilable y operativa con:

- catalogos en memoria
- auditoria en memoria
- scheduler de recarga cada 6 horas
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
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashin.item`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashin.path`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashin.capabilities`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashin.name`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashout.item`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashout.path`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashout.capabilities`
- `app.integration.providers.ecuabet.services.<CAPABILITY>.cashout.name`
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
- `APP_INTEGRATIONS_DEFAULT_CONNECT_TIMEOUT_MS`
- `APP_INTEGRATIONS_DEFAULT_READ_TIMEOUT_MS`
- `APP_INTEGRATIONS_MOCK_ENABLED`
- `APP_INTEGRATION_PROVIDERS_DEFAULT_BASE_URL`
- `APP_INTEGRATION_PROVIDERS_DEFAULT_TECHNICAL_USER`
- `APP_INTEGRATION_PROVIDERS_ECUABET_BASE_URL`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICE_PROVIDER_CODE`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SHOP_ID`
- `APP_INTEGRATION_PROVIDERS_ECUABET_COUNTRY`
- `APP_INTEGRATION_PROVIDERS_ECUABET_TOKEN`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_PRECHECK_CASHIN_PATH`
- `APP_INTEGRATION_PROVIDERS_ECUABET_SERVICES_PRECHECK_CASHOUT_PATH`

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
  "service_provider_code": "1",
  "rms_item_code": "10001565826",
  "userid": "997561",
  "phone": "123456",
  "document": "0912345678"
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

## Recarga de catalogos

- Se ejecuta una carga inicial al arrancar la aplicacion.
- El scheduler refresca el snapshot segun `app.catalog.refresh.fixed-delay-ms`.
- Si una recarga falla, se conserva la ultima version valida en memoria.

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
- `movement_type` no es obligatorio en `precheck`; la aplicacion usa la definicion del servicio resuelta desde catalogo/business-lines
- resolucion de ruta: OMNISTACK usa `provider -> capability -> flow(cashin/cashout)`, validando el `item` configurado contra el `rms_item_code` del servicio

El adapter HTTP real invoca `https://apidev.virtualsoft.tech/operatorapi-new/user/search` o la URL configurada por propiedades.

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
  "movement_type": "CASH_OUT",
  "category_code": "1",
  "subcategory_code": "1",
  "service_provider_code": "1",
  "rms_item_code": "10001565827",
  "withdrawId": "7667",
  "password": "88422"
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

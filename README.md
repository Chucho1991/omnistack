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

## Build y pruebas

```bash
mvn clean test
```

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
      "category_code": "REC",
      "category_name": "Recargas",
      "subcategory_code": "CEL",
      "subcategory_name": "Recargas celulares",
      "is_active": true,
      "service_providers": [
        {
          "service_provider_code": "CLARO",
          "provider_name": "Claro",
          "is_active": true,
          "services": [
            {
              "rms_item_code": "900001",
              "description": "Recarga Claro",
              "is_active": true,
              "jde_code": "JDE-REC-001",
              "movement_type": "CASH_IN",
              "is_mixed_payment": false,
              "flg_item": "RECA",
              "is_refund": false,
              "min_amount": "1.00",
              "max_amount": "200.00",
              "capabilities": [
                "PRECHECK",
                "EXECUTE",
                "VERIFY",
                "REVERSE"
              ],
              "input_fields": [
                {
                  "id": "phone",
                  "label": "Telefono",
                  "type": "STRING",
                  "capability": "PRECHECK",
                  "required": true,
                  "group": "PHONE"
                }
              ],
              "payment_methods": [
                {
                  "service_payment_method_id": 1,
                  "payment_method_code": "EFECTIVO",
                  "is_active": true
                }
              ],
              "requires_consent": false
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
  "chain": "001",
  "store": "0001",
  "storeName": "Tienda Centro",
  "pos": "POS-01",
  "channelPos": "POS",
  "movementType": "CASH_IN",
  "categoryCode": "REC",
  "subcategoryCode": "CEL",
  "serviceProviderCode": "CLARO",
  "rmsItemCode": "900001",
  "amount": 25.50,
  "phone": "0999999999"
}
```

### Response base transaccional

```json
{
  "is_error": false,
  "status": {
    "code": "00",
    "message": "PRECHECK completado correctamente"
  },
  "uuid": "f0908f64-9145-45cf-a22c-c36bca604372",
  "transactionId": "f0908f64-9145-45cf-a22c-c36bca604372",
  "providerCode": "OK",
  "providerMessage": "Operacion aprobada por proveedor mock"
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

No hay logica por proveedor en los controllers. En esta fase se entrega un adapter mock para proveedores REST.

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

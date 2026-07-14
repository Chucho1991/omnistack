# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service does

OmniStack is a Spring Boot 3.3.5 / Java 17 REST API that acts as a unified transaction orchestrator for a pharmacy POS system (Fybeca/GPF). The front-end (POS/caja) calls OmniStack's standardized endpoints; OmniStack routes each call to the correct external provider using a strategy pattern.

Providers implemented: **ECUABET** (REST), **LN BET593** (REST with managed session), **LN Pega3** (REST), **LN Tradicionales** (REST), **CLARO** (SOAP/XML — stubs only).

Package root: `com.omnistack.backend`

The critical business context — field mappings, provider contracts, and integration sequences — lives in `docs/CLAUDE.md`. Read that file before touching any provider adapter.

## Commands

```bash
./mvnw clean package -DskipTests   # build JAR
./mvnw spring-boot:run              # run (dev profile, port 8086)
./mvnw test                         # all tests
./mvnw test -Dtest=ClassName        # single test class
./mvnw test -Dtest=ClassName#method # single test method
```

Swagger UI: `http://localhost:8086/swagger-ui.html`  
API docs: `http://localhost:8086/api-docs`  
Admin diagnostic: `GET http://localhost:8086/v1/admin/item-config/{rmsItemCode}` (Swagger tag: "Admin - Diagnostico")

For local dev without real providers, set `APP_INTEGRATIONS_MOCK_ENABLED=true` (or copy `.env.example`). This activates `MockExternalProviderClient` instead of real WebClient adapters. Also activates `DefaultProviderTransactionStrategy`, which always returns `false` from `supports()` (intentional — forces an explicit config error if no real strategy matches).

In dev/QA, set `app.integrations.ssl-verification-disabled=true` to bypass the SSL handshake error from `www8.loteria.com.ec` (Java 17 TLS issue).

### Docker

```bash
docker build -t omnistack-backend:local .
docker run -d --name omnistack --env-file .env -p 8086:8086 omnistack-backend:local
```

## Architecture

Hexagonal / clean architecture with strict layer separation:

```
controller  →  application (use cases, ports/in)
               application (ports/out) ←→ infrastructure (adapters)
               domain (models, enums — no Spring deps)
shared      (exceptions, validation, logging, constants)
config      (Spring beans, OpenAPI, WebClient, datasource)
```

### Transaction flow

All transaction endpoints (`/v1/preCheck`, `/v1/execute`, `/v1/verify`, `/v1/reverse`, `/v1/createTicket`) follow the same path:

1. `TransactionController` validates input and delegates to `TransactionOrchestrationService`
2. `DefaultProviderFlowResolver.resolve(request, capability)` looks up the `ServiceDefinition` from `CatalogCacheService` (keyed by `categoryCode + subcategoryCode + serviceProviderCode + rmsItemCode`), then picks the first `TransactionFlowStrategy` bean whose `supports()` returns true
3. The selected strategy calls the provider HTTP adapter, wrapping it with `ProviderCircuitBreaker.execute(providerKey, supplier)` — 50% failure rate in 10-call window opens the circuit for 30 s
4. Each external HTTP call logs to `WsExtLogService.log(ProviderCallLog)` (async via `loggingExecutor`) which writes to `IN_OMNI_LOGS_WS_EXT`
5. `OracleAuditLogAdapter` saves to `IN_OMNI_LOGS_APP` (async, `loggingExecutor`)
6. `OracleRegistroTrxAdapter` saves to `IN_OMNI_REGISTRO_TRX` for EXECUTE, CREATE_TICKET, and REVERSE (not precheck/verify)
7. If `ServiceDefinition.homologatedAuth` is `true`, `TransactionOrchestrationService` generates a homologated code (10-char alphanumeric), stores the original in `AUTHORIZATION` and the homologated code in `CP_VAR1`, then returns the homologated code to the POS

### Cash-out daily quota control

For `CASH_OUT` services (`MovementType.CASH_OUT`), `TransactionOrchestrationService` enforces a daily quota limit per store (farmacia) before and after provider invocation:

**PRECHECK:** `CashOutQuotaService.reserveQuota()` validates:
1. Transaction amount ≤ `MONTO_MAX` (max per transaction)
2. Transaction amount ≤ available daily quota (`MONTO_MAX` − already consumed)

If valid, inserts a `RESERVADO` row in `IN_OMNI_CASHOUT_CUPO_DIARIO`. If not, throws `BusinessException` (HTTP 422).

**EXECUTE:** `CashOutQuotaService.confirmQuota()` changes the entry from `RESERVADO` → `CONFIRMADO`.

**REVERSE:** `CashOutQuotaService.revertQuota()` changes the entry to `REVERTIDO` **only if the reverse happens on the same calendar day** as the original transaction. If the reverse is on a later date, the quota is NOT restored (it belonged to a different day's allocation).

**Expiration scheduler:** `CashOutQuotaExpirationScheduler` runs every `app.cashout-quota.expiration-scheduler-rate-ms` (default 60s) and expires any `RESERVADO` entry older than `app.cashout-quota.reservation-timeout-minutes` (default 30 min), setting its state to `EXPIRADO` and restoring the quota.

Key classes: `CashOutQuotaService`, `CashOutQuotaPort`, `OracleCashOutQuotaAdapter`, `CashOutQuotaExpirationScheduler`, `CashOutQuotaEntry`, `CashOutQuotaStatus`.

Table: `TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO` (DDL: `docs/bdd/omnistack/26_DDL_CASHOUT_CUPO_DIARIO.sql`).

### Transaction amount validation (non-CASH_OUT)

For all services that are NOT `CASH_OUT`, `TransactionOrchestrationService` enforces per-transaction amount limits before calling the provider:

**PRECHECK and EXECUTE:** `TransactionAmountValidationService.validate()` checks:
1. Transaction amount ≤ `MONTO_MAX` from `AD_SERVICIO_PARAMETROS` (via `ServiceDefinition.getMaxAmount()`)
2. Transaction amount ≥ `MONTO_MIN` from `AD_SERVICIO_PARAMETROS` (via `ServiceDefinition.getMinAmount()`)

If the amount violates either limit, throws `BusinessException` (HTTP 422) with a descriptive message including the amount, the limit, and the item code.

This control is per-transaction only — no daily accumulation or quota reservation is involved (unlike CASH_OUT). CASH_OUT items are excluded because they already validate the amount inside `CashOutQuotaService.reserveQuota()`.

Key class: `TransactionAmountValidationService`.

### Homologated authorization code

When `AD_SERVICIO_PARAMETROS.ID_HOMOLOGADO = 'S'` for a given item, OmniStack generates an internal authorization code instead of exposing the provider's raw code to the POS.

**EXECUTE / CREATE_TICKET flow:**
1. `ServiceDefinition.isHomologatedAuth()` returns `true`
2. Strategy executes normally — provider returns its original authorization
3. `HomologatedCodeService.generate()` produces a 10-char alphanumeric code (timestamp base-36 + random)
4. `IN_OMNI_REGISTRO_TRX`: `AUTHORIZATION` = provider's original, `CP_VAR1` = homologated code
5. Response to POS: `authorization` field = homologated code

**REVERSE flow:**
1. POS sends the homologated code in `authorization`
2. `TransactionOrchestrationService.resolveOriginalAuthForReverse()` queries `IN_OMNI_REGISTRO_TRX` by `CP_VAR1`
3. Replaces `request.authorization` with the original provider code
4. Strategy sends the original to the provider

Key classes: `HomologatedCodeService` (generation), `RegistroTrxPort.findOriginalAuthByHomologatedCode()` (resolution), `TransactionOrchestrationService` (orchestration).

### Catalog — two separate caches

There are **two independent in-memory caches**; do not confuse them:

| Service | Purpose | Refresh |
|---|---|---|
| `CatalogCacheService` | Routing — holds `ServiceDefinition` list used to resolve strategy per request | `CatalogRefreshScheduler` every 6 h |
| `BusinessLinesCatalogCacheService` | Display — holds the full `/business-lines` response tree (categories → subcategories → providers → services) | same scheduler |

Both `CatalogCacheService` and `BusinessLinesCatalogCacheService` source from the same adapter: `OracleBusinessLinesCatalogSourceAdapter` (implements both `CatalogSourcePort` and `BusinessLinesCatalogSourcePort`). It runs the SQL files in `src/main/resources/sql/business-lines/oracle/` — six prod queries (`category-subcategory.sql`, `service-providers.sql`, `services.sql`, `capabilities.sql`, `input-fields.sql`, `payment-methods.sql`) and six RMS join queries (`ad-services.sql`, `ad-capabilities.sql`, `ad-movement-types.sql`, `ad-payment-methods.sql`, `rms-items.sql`, `rms-suppliers.sql`). Active when `app.business-lines.source=oracle` (the default). Fallbacks: `InMemoryCatalogSourceAdapter` and `InMemoryBusinessLinesCatalogSourceAdapter` when `source=memory`.

### DB-driven provider configuration (three in-memory caches)

All provider config is loaded from Oracle at startup and held in memory. No `@Value` injection for provider secrets — use these services in strategies:

| Service | Source table | Cache key format | Purpose |
|---|---|---|---|
| `ProviderConfigService` | `IN_OMNI_PROVEEDOR_CONFIG` | `providerkey\|config_key` | Functional config (tokens, shop IDs, credentials) |
| `ProviderWsService` | `IN_OMNI_PROVEEDOR_WS` | `providerkey\|WSKEY` | Endpoint URLs per operation |
| `ProviderWsDefsService` | `IN_OMNI_PROVEEDOR_WS_DEFS` | `providerkey\|WSKEY\|field` | Per-operation defaults (items, offer IDs, medio_id) |

`ProviderConfigService.getProviderProperties(providerKey)` builds a full `AppProperties.ProviderProperties` from DB rows. Returns `null` if the provider has no config — `supports()` must handle this case with `findProviderProperties` (not `getProviderProperties`).

### Strategy base class

All strategies extend `AbstractProviderStrategy`. Key inherited methods:

```java
// In supports():
findProviderProperties(configService, "ecuabet")   // returns null if not configured
hasConfiguredOperation(wsService, defsService, "ecuabet", capability, serviceDefinition)

// In process():
getProviderProperties(configService, "ecuabet", "ECUABET")  // throws if not configured
getRequiredOperationUrl(wsService, defsService, "ecuabet", capability, serviceDefinition, "ECUABET")
validateValue(fieldName, currentValue, expectedValue, providerName)
stringValue(payload, key) / resolveValue(payload, key, fallback) / integerValue / decimalValue

// Resolve a per-item field from WS_DEFS, falling back to an explicit request value:
resolveItemDefault(explicitValue, defsService, providerKey, wsKey, "juego_id", rmsItemCode, providerName)
// Throws IntegrationException if neither the explicit value nor the DB default is present.
// Uses the multi-item format key: "{fieldPrefix}.{rmsItemCode}" in IN_OMNI_PROVEEDOR_WS_DEFS.
```

`toWsKey(capability.name(), movementType)` builds the lookup key: `PRECHECK.CASHIN`, `EXECUTE.CASHOUT`, etc. This is the key used in `IN_OMNI_PROVEEDOR_WS` and `IN_OMNI_PROVEEDOR_WS_DEFS`.

WS_DEFS supports two item formats:
- **Multi-item** (current): `DEFAULT_CLAVE = "item.{rmsItemCode}"` — N items per WS_KEY
- **Single-item** (legacy): `DEFAULT_CLAVE = "item"`, `DEFAULT_VALOR_TEXT = rmsItemCode`

### Error code contract

`CanonicalErrorCodeMapper.resolve(externalResponse)` maps every provider response to three canonical codes (from `ErrorCodes`):

| Code | Constant | Meaning |
|---|---|---|
| `"00"` | `ErrorCodes.OK` | Success |
| `"01"` | `ErrorCodes.ERROR_DESCRIPTION_OBTAINED` | Generic error — use `externalMessage` |
| `"02"` | `ErrorCodes.INVALID_USER` | User not found / invalid |

`GlobalExceptionHandler` maps Java exceptions to HTTP status codes:

| Exception | HTTP |
|---|---|
| `MethodArgumentNotValidException` / `BindException` / `ConstraintViolationException` | 400 |
| `BusinessException` | 422 |
| `IntegrationException` | 502 |
| `CatalogNotFoundException` | 404 |
| Uncaught `RuntimeException` | 500 |

### Oracle datasources

Two named datasources, both `@ConditionalOnProperty` on their URL:

| Bean qualifier | Schema | Tables | Condition |
|---|---|---|---|
| `prodOracleJdbcTemplate` | `TUKUNAFUNC` (prod) / `GPF_OMNISTACK` (QA) | `IN_OMNI_*` (config, logs) | `app.datasource.prod.url` |
| `rmsOracleJdbcTemplate` | `gpf_lectura` (QA) / `rms` (dev local) | `AD_*`, `RMS.*` catalog tables | `app.datasource.rms.url` |

Oracle beans (`OracleAuditLogAdapter`, `OracleProviderWsAdapter`, etc.) are all annotated with `@ConditionalOnProperty(name = "app.datasource.prod.url")`. When that property is absent, NoOp adapters (`NoOpWsExtLogAdapter`, `NoOpRegistroTrxAdapter`) activate instead. `InMemoryAuditLogAdapter` is the fallback for `AuditLogPort`.

Audit log inserts use `SELECT NVL(MAX(CODIGO), 0) + 1 FROM table` as a PK sequence (no IDENTITY columns). CLOB fields must use `Types.CLOB` in `MapSqlParameterSource.addValue`.

### Oracle logging tables (all in TUKUNAFUNC schema)

| Table | Adapter | Trigger |
|---|---|---|
| `IN_OMNI_LOGS_APP` | `OracleAuditLogAdapter` | Every transaction (EXECUTE, PRECHECK, VERIFY, REVERSE) — async |
| `IN_OMNI_LOGS_WS_EXT` | `OracleWsExtLogAdapter` | Every external HTTP call to provider — async via `WsExtLogService.log()` |
| `IN_OMNI_REGISTRO_TRX` | `OracleRegistroTrxAdapter` | EXECUTE, CREATE_TICKET, REVERSE on success — async |
| `IN_OMNI_CASHOUT_CUPO_DIARIO` | `OracleCashOutQuotaAdapter` | PRECHECK (reserve), EXECUTE (confirm), REVERSE (revert) for CASH_OUT — sync |

### Admin / Diagnostic endpoints

`GET /v1/admin/item-config/{rmsItemCode}` queries all 7 tables involved in item parametrization (AD_SERVICIO_PARAMETROS, ITEM_MASTER, ITEM_SUPPLIER, IN_OMNI_PROVEEDOR_CONFIG, IN_OMNI_PROVEEDOR_WS, IN_OMNI_PROVEEDOR_WS_DEFS, IN_OMNI_INPUT_FIELDS) and returns a JSON with data classified by source table plus an automatic diagnostic of missing configuration.

Classes: `ItemConfigDiagnosticController`, `ItemConfigDiagnosticService`, `OracleItemConfigDiagnosticAdapter`.

### Provider token management

Providers that require a login session (Lotería) are handled by `ProviderTokenService`. Tokens are refreshed at startup and on a schedule by `ProviderTokenRefreshScheduler`. Ecuabet uses a static token from config (`auth.mode=STATIC`); Lotería uses dynamic login (`auth.mode=LOGIN`). The token lives in `IN_OMNI_PROVEEDOR_CONFIG` row with `config_key=token`.

Token resolution in adapters: use `ProviderTokenResolverUseCase.getToken(String providerKey)` to resolve the active token directly by provider key (e.g., `"tradicional"`, `"pega3"`, `"loteria"`). This bypasses the category/subcategory lookup used by the controller endpoint. On receiving a token-expired error from the provider, adapters call `refreshToken(providerKey)` and retry the call once.

`ProviderTokenController` exposes `POST /v1/provider-token/refresh` for on-demand token refresh (useful when a provider session expires unexpectedly in prod).

`CatalogCacheHealthIndicator` integrates with Spring Actuator (`/actuator/health`) and reports `DOWN` when the catalog snapshot is empty or stale.

## Application profiles

Three Spring profiles; active profile set via `SPRING_PROFILES_ACTIVE` env var (default `dev`):

| Profile | Port | Oracle prod URL | Oracle RMS URL | SSL bypass |
|---|---|---|---|---|
| `dev` | 8086 | `localhost:1521/XEPDB1` (TUKUNAFUNC) | `localhost:1521/XEPDB1` (rms) | `true` |
| `qa` | 8086 | `10.100.3.20:1521:PRS6` (TUKUNAFUNC) | `vmcluts1scan.gfybeca.int/momqa_pdb1` (gpf_lectura) | `true` |
| `prod` | 8086 | via env vars only | via env vars only | `false` |

All connection parameters in non-prod profiles are overridable via env vars (`APP_DATASOURCE_PROD_URL`, etc.).

## DB migration scripts

Numbered SQL scripts must be run in order:

- `docs/bdd/local-setup/` — one-time local dev environment (admin, RMS DDL, grants)
- `docs/bdd/omnistack/` — OmniStack schema DDL + DML (01 = DDL, 02+ = data/fixes)
  - Script `25` adds `ID_HOMOLOGADO` column to `AD_SERVICIO_PARAMETROS`
  - Script `26` creates `IN_OMNI_CASHOUT_CUPO_DIARIO` table for CASH_OUT daily quota control

When adding a new provider, append new numbered scripts to `docs/bdd/omnistack/` — never modify existing ones.

## Adding a new provider

1. Define domain commands in `domain/model/` (e.g., `ClaroRechargeCommand.java`)
2. Define output ports in `application/port/out/` (one per operation)
3. Extend `AbstractProviderStrategy` and implement one of `PrecheckStrategy`, `ExecuteStrategy`, `VerifyStrategy`, `ReverseStrategy` in `infrastructure/adapter/integration/`
4. `supports()` must call `findProviderProperties` (not `getProviderProperties`) and `hasConfiguredOperation` — both return false/null gracefully when DB has no config
5. In `process()`, wrap every external HTTP call with `providerCircuitBreaker.execute(PROVIDER_KEY, () -> adapter.call(...))` and log it with `wsExtLogService.log(ProviderCallLog.builder()...build())`
6. Add provider-specific DTOs in `infrastructure/adapter/integration/<provider>/dto/`
7. Implement WebClient adapters in `infrastructure/adapter/integration/<provider>/`
8. Insert rows into `IN_OMNI_PROVEEDOR_CONFIG`, `IN_OMNI_PROVEEDOR_WS`, `IN_OMNI_PROVEEDOR_WS_DEFS` in a new numbered `docs/bdd/omnistack/` script
9. Update Swagger docs, README.md, AGENTS.md, and Postman collection (per AGENTS.md checklist)

## Key conventions

- All amounts: `BigDecimal` with 2 decimals, dot separator (`1000.00`); `double` only appears in legacy DTOs
- `uuid` from the request propagates to every external provider as its correlation ID (`codigotrn` for LN, `EXTERNALTRANSACTIONID` for Claro)
- `authorization` in every OmniStack response holds the provider's tracking ID, regardless of what the provider names it — **unless** the item has `ID_HOMOLOGADO = 'S'`, in which case it holds the homologated code and the original is stored in `IN_OMNI_REGISTRO_TRX.AUTHORIZATION`
- Internal credentials (tokens, shop IDs, API keys) are **never** passed from the front-end — they come from `ProviderConfigService`
- `movement_type` in `BaseTransactionRequest` is optional — OmniStack resolves it from the catalog and sets it on the request before invoking the strategy
- Lombok throughout; no manual getters/setters
- No JPA entities in `domain`; persistence adapters map explicitly
- Strategy provider keys are lowercase strings matching `IN_OMNI_PROVEEDOR_CONFIG.PROVEEDOR_KEY`: `"ecuabet"`, `"loteria"`, `"pega3"`, `"tradicional"`, `"claro"`

## Test conventions

- Strategy tests use `@ExtendWith(MockitoExtension.class)` + `@MockitoSettings(strictness = Strictness.LENIENT)` — required because `supports()` stubs are also used in `process()` tests
- Use `wsService.requireUrl(...)` in `process()` (throws on missing URL); use `wsService.findUrl(...)` or `hasUrl(...)` in `supports()` (returns Optional/boolean)
- `anyString()` does not match `null` — stub with `eq(null)` or `isNull()` when the field may be null

## Reference docs in `docs/`

| File | When to read |
|---|---|
| `docs/CLAUDE.md` | Full integration context — read before any provider work |
| `docs/MapeoCampos_v8.xlsx` | Field-by-field mapping OmniStack ↔ provider per phase |
| `docs/document_pdf.pdf` | CLARO SOAP technical spec (XML schema, field definitions, offer IDs) |
| `docs/OmniStack_postman_collection_v8.json` | Request/response examples with real data |
| `docs/OmniStack-TST_postman_environment_v3.json` | Dev URLs and credentials by provider |

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service does

OmniStack is a Spring Boot 3.3.5 / Java 17 REST API that acts as a unified transaction orchestrator for a pharmacy POS system (Fybeca/GPF). The front-end (POS/caja) calls OmniStack's standardized endpoints; OmniStack routes each call to the correct external provider using a strategy pattern. Providers currently implemented: **ECUABET** (REST) and **LN BET593** (REST with managed login session). Providers planned but not yet implemented: LN Pega3, LN Tradicionales, CLARO (SOAP/XML).

The critical business context — field mappings, provider contracts, and integration sequences — lives in `docs/CLAUDE.md`. Read that file before touching any provider adapter.

## Commands

```bash
./mvnw clean package -DskipTests   # build JAR
./mvnw spring-boot:run              # run (dev profile, port 8085)
./mvnw test                         # all tests
./mvnw test -Dtest=ClassName        # single test class
./mvnw test -Dtest=ClassName#method # single test method
```

Swagger UI: `http://localhost:8085/swagger-ui.html`  
API docs: `http://localhost:8085/api-docs`

For local dev without real providers, set `APP_INTEGRATIONS_MOCK_ENABLED=true` (or copy `.env.example` and set it there). This activates `MockExternalProviderClient` instead of real WebClient adapters.

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

All transaction endpoints (`/v1/preCheck`, `/v1/execute`, `/v1/verify`, `/v1/reverse`) follow the same path:

1. `TransactionController` validates input and delegates to `TransactionUseCase`
2. `TransactionOrchestrationService` calls `ProviderFlowResolver.resolve(request, capability)`
3. `DefaultProviderFlowResolver` looks up the `ServiceDefinition` from the in-memory catalog (keyed by `categoryCode + subcategoryCode + serviceProviderCode + rmsItemCode`), then picks the first `TransactionFlowStrategy` bean whose `supports()` returns true
4. The selected strategy executes the provider-specific logic and returns the typed response
5. The orchestrator saves an `TransactionAuditLog` entry (currently in-memory)

### Catalog

`/business-lines` returns the full service catalog from Oracle (via `OracleBusinessLinesCatalogSourceAdapter`) or in-memory fixtures. The catalog is cached in `CatalogCacheService` and refreshed every 6 hours by `CatalogRefreshScheduler`. The catalog drives routing — `ServiceDefinition.capabilities` determines which capabilities (`PRECHECK`, `EXECUTE`, `VERIFY`, `REVERSE`) are valid for a given service.

### Provider routing

Provider is selected by `service_provider_code` in the request, which maps to a `ServiceDefinition` in the catalog. The `TransactionFlowStrategy.supports(serviceDefinition, capability)` method on each strategy bean determines whether it handles a given provider+capability combination.

Current strategies:
- `EcuabetPrecheckStrategy` / `EcuabetDepositExecuteStrategy` / `EcuabetWithdrawExecuteStrategy` / `EcuabetDepositReverseStrategy` / `EcuabetWithdrawReverseStrategy`
- `LoteriaBet593PrecheckStrategy` / `LoteriaBet593ExecuteStrategy` / `LoteriaBet593WithdrawExecuteStrategy` / `LoteriaBet593VerifyStrategy` / `LoteriaBet593WithdrawVerifyStrategy` / `LoteriaBet593RechargeReverseStrategy` / `LoteriaBet593WithdrawReverseStrategy`

### Provider token management

Providers that require a login session (Lotería) are handled by `ProviderTokenService`. Tokens are refreshed at startup and on a schedule by `ProviderTokenRefreshScheduler`. Ecuabet uses a static token from config (`auth.mode=STATIC`); Lotería uses dynamic login (`auth.mode=LOGIN`).

## Adding a new provider

1. Define domain commands in `domain/model/` (e.g., `ClaroRechargeCommand.java`)
2. Define output ports in `application/port/out/` (one per operation)
3. Add strategy interface implementations in `infrastructure/adapter/integration/` — implement `TransactionFlowStrategy` with a `supports()` method that identifies this provider
4. Add provider-specific DTOs in `infrastructure/adapter/integration/<provider>/dto/`
5. Implement WebClient adapters in `infrastructure/adapter/integration/<provider>/`
6. Add provider config to `AppProperties` and `application.properties` (all env-var backed)
7. Add unit tests for the strategy and WebClient adapter
8. Update Swagger docs, README.md, AGENTS.md, and Postman collection (per AGENTS.md checklist)

## Key conventions

- All amounts: `double` with 2 decimals, dot separator, no thousands separator (`1000.00`)
- `uuid` from the request propagates to every external provider as its correlation ID (`codigotrn` for LN, `EXTERNALTRANSACTIONID` for Claro)
- `authorization` in every OmniStack response holds the provider's tracking ID, regardless of what the provider names it
- Internal credentials (tokens, shop IDs, API keys) are **never** passed from the front-end — they come from config only
- Lombok throughout; no manual getters/setters
- No JPA entities in `domain`; persistence adapters map explicitly

## Reference docs in `docs/`

| File | When to read |
|---|---|
| `docs/CLAUDE.md` | Full integration context — read before any provider work |
| `docs/MapeoCampos_v8.xlsx` | Field-by-field mapping OmniStack ↔ provider per phase |
| `docs/OmniStack_postman_collection_v8.json` | Request/response examples with real data |
| `docs/OmniStack-TST_postman_environment_v3.json` | Dev URLs and credentials by provider |

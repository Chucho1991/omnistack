package com.omnistack.backend.infrastructure.adapter.integration.ecuabet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.EcuabetUserSearchPort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.WsExtLogService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetUserSearchCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ProviderCallLog;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetErrorResponse;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetUserSearchRequest;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetUserSearchResponse;
import com.omnistack.backend.shared.constants.ErrorCodes;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adapter HTTP para la operacion Buscar usuario de ECUABET.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcuabetUserSearchWebClientAdapter implements EcuabetUserSearchPort {

    private static final String PROVIDER_KEY = "ecuabet";
    private static final String WS_KEY_CASHIN = "PRECHECK.CASHIN";
    private static final String WS_KEY_CASHOUT = "PRECHECK.CASHOUT";

    private final WebClient omnistackWebClient;
    private final ProviderConfigService providerConfigService;
    private final ObjectMapper objectMapper;
    private final ProviderTokenResolverUseCase providerTokenResolverUseCase;
    private final WsExtLogService wsExtLogService;

    @Override
    public ExternalTransactionResponse searchUser(EcuabetUserSearchCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        EcuabetUserSearchRequest request = buildExternalRequest(command, provider);
        String url = operationPath;
        String operationName = resolveOperationName(operationPath);

        String wsKey = command.getMovementType() == MovementType.CASH_OUT ? WS_KEY_CASHOUT : WS_KEY_CASHIN;
        long startMs = System.currentTimeMillis();
        String requestJson = JsonUtil.toJsonSilently(request);
        traceToConsole("External web service request", url, requestJson);

        EcuabetUserSearchResponse response = omnistackWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> addHeaders(headers, command))
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            traceErrorToConsole("External web service error", url, body);
                            wsExtLogService.log(ProviderCallLog.builder()
                                    .uuid(command.getUuid())
                                    .providerKey(PROVIDER_KEY)
                                    .wsKey(wsKey)
                                    .url(url)
                                    .requestJson(requestJson)
                                    .responseJson(body)
                                    .durationMs(System.currentTimeMillis() - startMs)
                                    .isError(true)
                                    .errorMessage(buildErrorMessage(body, operationName))
                                    .build());
                            return Mono.error(new IntegrationException(buildErrorMessage(body, operationName)));
                        }))
                .bodyToMono(EcuabetUserSearchResponse.class)
                .block();

        if (response == null) {
            throw new IntegrationException("ECUABET no retorno contenido para la operacion " + operationName);
        }

        String responseJson = JsonUtil.toJsonSilently(response);
        traceToConsole("External web service response", url, responseJson);
        wsExtLogService.log(ProviderCallLog.builder()
                .uuid(command.getUuid())
                .providerKey(PROVIDER_KEY)
                .wsKey(wsKey)
                .url(url)
                .requestJson(requestJson)
                .responseJson(responseJson)
                .durationMs(System.currentTimeMillis() - startMs)
                .isError(false)
                .build());

        return ExternalTransactionResponse.builder()
                .approved(!hasBusinessError(command, response))
                .externalCode(resolveExternalCode(response))
                .externalMessage(resolveExternalMessage(command, response))
                .payload(buildPayload(response))
                .build();
    }

    private void addHeaders(HttpHeaders headers, EcuabetUserSearchCommand command) {
        headers.add("chain", command.getChain());
        headers.add("store", command.getStore());
        if (command.getStoreName() != null) {
            headers.add("store_name", command.getStoreName());
        }
        headers.add("pos", command.getPos());
        headers.add("channel_POS", command.getChannelPos().name());
    }

    private EcuabetUserSearchRequest buildExternalRequest(
            EcuabetUserSearchCommand command,
            AppProperties.ProviderProperties provider) {
        String providerToken = providerTokenResolverUseCase.getToken(
                command.getCategoryCode(),
                command.getSubcategoryCode(),
                provider.getServiceProviderCode());
        if (command.getMovementType() == MovementType.CASH_OUT) {
            return EcuabetUserSearchRequest.builder()
                    .shop(provider.getShopId())
                    .token(providerToken)
                    .withdrawId(requiredValue(command.getWithdrawId(), "withdrawId"))
                    .country(provider.getCountry())
                    .password(requiredValue(command.getPassword(), "password"))
                    .build();
        }

        return EcuabetUserSearchRequest.builder()
                .shop(provider.getShopId())
                .token(providerToken)
                .userid(nullIfBlank(command.getUserid()))
                .country(provider.getCountry())
                .phone(nullIfBlank(command.getPhone()))
                .document(nullIfBlank(command.getDocument()))
                .build();
    }

    private String resolveExternalCode(EcuabetUserSearchResponse response) {
        if (isInvalidUserMessage(response.getMessage())) {
            return ErrorCodes.INVALID_USER;
        }
        if (response.getError() != null && response.getError() != 0) {
            return ErrorCodes.ERROR_DESCRIPTION_OBTAINED;
        }
        return response.getCode() != null && !response.getCode().isBlank() ? response.getCode() : "00";
    }

    private String resolveExternalMessage(EcuabetUserSearchCommand command, EcuabetUserSearchResponse response) {
        if (response.getMessage() != null && !response.getMessage().isBlank()) {
            return response.getMessage();
        }
        if (response.getError() != null && response.getError() != 0) {
            return String.valueOf(response.getError());
        }
        return isMissingLookupData(command, response)
                ? "ECUABET no retorno datos para aprobar el precheck"
                : "Operacion procesada por ECUABET";
    }

    private boolean hasBusinessError(EcuabetUserSearchCommand command, EcuabetUserSearchResponse response) {
        if (response.getError() != null && response.getError() != 0) {
            return true;
        }
        String code = response.getCode();
        return code != null && !code.isBlank() && !isSuccessfulCode(code)
                || isMissingLookupData(command, response);
    }

    private boolean isSuccessfulCode(String code) {
        return "0".equals(code.trim()) || "00".equals(code.trim());
    }

    private boolean isInvalidUserMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("usuario")
                && (normalized.contains("invalido")
                || normalized.contains("inválido")
                || normalized.contains("no encontrado")
                || normalized.contains("no existe"));
    }

    private boolean isMissingLookupData(EcuabetUserSearchCommand command, EcuabetUserSearchResponse response) {
        if (command.getMovementType() == MovementType.CASH_OUT) {
            return isBlank(response.getName())
                    && isBlank(response.getUserid())
                    && response.getRaw().get("amount") == null
                    && response.getRaw().get("currency") == null;
        }
        return isBlank(response.getName()) && isBlank(response.getUserid());
    }

    private String buildErrorMessage(String body, String operationName) {
        if (body == null || body.isBlank()) {
            return "Error HTTP al invocar ECUABET " + operationName;
        }

        try {
            EcuabetErrorResponse error = objectMapper.readValue(body, EcuabetErrorResponse.class);
            if (error.getMessage() != null && !error.getMessage().isBlank()) {
                return "ECUABET " + operationName + " respondio con error: " + error.getMessage();
            }
            if (error.getError() != null && !error.getError().isBlank()) {
                return "ECUABET " + operationName + " respondio con error: " + error.getError();
            }
            return "ECUABET " + operationName + " respondio con error: " + objectMapper.writeValueAsString(error.getRaw());
        } catch (JsonProcessingException exception) {
            return "ECUABET " + operationName + " respondio con error no parseable";
        }
    }

    private Map<String, Object> buildPayload(EcuabetUserSearchResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", response.getCode());
        payload.put("error", response.getError());
        payload.put("message", response.getMessage());
        payload.put("name", response.getName());
        payload.putAll(response.getRaw());
        payload.put("userid", response.getUserid());
        return payload;
    }

    private String resolveOperationName(String operationPath) {
        if (operationPath == null || operationPath.isBlank()) {
            return "operacion-configurada";
        }
        return operationPath.startsWith("/") ? operationPath.substring(1) : operationPath;
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(PROVIDER_KEY);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor ECUABET");
        }
        return provider;
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String requiredValue(String value, String fieldName) {
        String normalizedValue = nullIfBlank(value);
        if (normalizedValue == null) {
            throw new IntegrationException("ECUABET requiere el campo " + fieldName + " para la operacion de CASH_OUT");
        }
        return normalizedValue;
    }

    private void traceToConsole(String label, String url, String body) {
        log.info("{} url={} body={}", label, url, body);
    }

    private void traceErrorToConsole(String label, String url, String body) {
        log.error("{} url={} body={}", label, url, body);
    }
}

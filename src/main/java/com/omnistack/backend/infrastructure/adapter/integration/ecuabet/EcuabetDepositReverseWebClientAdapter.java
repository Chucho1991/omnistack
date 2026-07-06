package com.omnistack.backend.infrastructure.adapter.integration.ecuabet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.EcuabetDepositReversePort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.WsExtLogService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.EcuabetDepositCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ProviderCallLog;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetDepositResponse;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetDepositReverseRequest;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetErrorResponse;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Adapter HTTP para el reverso de deposito ECUABET.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcuabetDepositReverseWebClientAdapter implements EcuabetDepositReversePort {

    private static final String PROVIDER_KEY = "ecuabet";
    private static final String WS_KEY = "REVERSE.CASHIN";

    private final WebClient omnistackWebClient;
    private final ProviderConfigService providerConfigService;
    private final ObjectMapper objectMapper;
    private final ProviderTokenResolverUseCase providerTokenResolverUseCase;
    private final WsExtLogService wsExtLogService;

    /**
     * Ejecuta el consumo externo de reverso de deposito ECUABET.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    @Override
    public ExternalTransactionResponse reverseDeposit(EcuabetDepositCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        EcuabetDepositReverseRequest request = buildExternalRequest(command, provider);
        String url = operationPath;

        long startMs = System.currentTimeMillis();
        String requestJson = JsonUtil.toJsonSilently(request);
        traceToConsole("ECUABET deposit reverse request", url, requestJson);

        EcuabetDepositResponse response;
        try {
            response = omnistackWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> addHeaders(headers, command))
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                traceErrorToConsole("ECUABET deposit reverse error", url, body);
                                wsExtLogService.log(ProviderCallLog.builder()
                                        .uuid(command.getUuid())
                                        .providerKey(PROVIDER_KEY)
                                        .wsKey(WS_KEY)
                                        .url(url)
                                        .requestJson(requestJson)
                                        .responseJson(body)
                                        .durationMs(System.currentTimeMillis() - startMs)
                                        .isError(true)
                                        .errorMessage(buildErrorMessage(body))
                                        .build());
                                return Mono.error(new IntegrationException(buildErrorMessage(body)));
                            }))
                    .bodyToMono(EcuabetDepositResponse.class)
                    .block();
        } catch (WebClientRequestException exception) {
            String message = EcuabetTransportErrorMapper.buildMessage("reverso de deposito", url, exception);
            traceErrorToConsole("ECUABET deposit reverse transport error", url, message);
            throw new IntegrationException(message, exception);
        }

        if (response == null) {
            throw new IntegrationException("ECUABET no retorno contenido para reverso de deposito");
        }

        String responseJson = JsonUtil.toJsonSilently(response);
        traceToConsole("ECUABET deposit reverse response", url, responseJson);
        wsExtLogService.log(ProviderCallLog.builder()
                .uuid(command.getUuid())
                .providerKey(PROVIDER_KEY)
                .wsKey(WS_KEY)
                .url(url)
                .requestJson(requestJson)
                .responseJson(responseJson)
                .durationMs(System.currentTimeMillis() - startMs)
                .isError(false)
                .build());

        return ExternalTransactionResponse.builder()
                .approved((response.getError() == null || response.getError() == 0)
                        && isSuccessfulCode(resolveExternalCode(response)))
                .externalCode(resolveExternalCode(response))
                .externalMessage(resolveExternalMessage(response))
                .payload(buildPayload(command, response))
                .build();
    }

    private void addHeaders(HttpHeaders headers, EcuabetDepositCommand command) {
        headers.add("chain", command.getChain());
        headers.add("store", command.getStore());
        if (command.getStoreName() != null) {
            headers.add("store_name", command.getStoreName());
        }
        headers.add("pos", command.getPos());
        headers.add("channel_POS", command.getChannelPos().name());
    }

    private EcuabetDepositReverseRequest buildExternalRequest(
            EcuabetDepositCommand command,
            AppProperties.ProviderProperties provider) {
        validateProviderConfiguration(provider);
        String providerToken = providerTokenResolverUseCase.getToken(PROVIDER_KEY);

        return EcuabetDepositReverseRequest.builder()
                .shop(provider.getShopId())
                .token(providerToken)
                .country(provider.getCountry())
                .amount(requiredAmount(command))
                .transactionId(requiredTransactionId(command))
                .build();
    }

    private Map<String, Object> buildPayload(EcuabetDepositCommand command, EcuabetDepositResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", response.getError());
        payload.put("code", resolveExternalCode(response));
        payload.put("message", resolveExternalMessage(response));
        payload.put("name", response.getName());
        payload.put("lastname", response.getLastname());
        payload.put("currency", response.getCurrency());
        payload.put("authorization", String.valueOf(command.getTransactionId()));
        payload.put("document", command.getDocument());
        payload.put("amount", command.getAmount());
        payload.put("providerTransactionId", response.getTransactionId());
        payload.putAll(response.getRaw());
        return payload;
    }

    private String resolveExternalCode(EcuabetDepositResponse response) {
        return response.getCode() != null && !response.getCode().isBlank() ? response.getCode() : "0";
    }

    private String resolveExternalMessage(EcuabetDepositResponse response) {
        if (response.getMessage() != null && !response.getMessage().isBlank()) {
            return response.getMessage();
        }
        return response.getError() != null && response.getError() != 0
                ? String.valueOf(response.getError())
                : "Transaccion correcta";
    }

    private boolean isSuccessfulCode(String code) {
        return "0".equals(code.trim()) || "00".equals(code.trim());
    }

    private String buildErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Error HTTP al invocar reverso de deposito ECUABET";
        }

        try {
            EcuabetErrorResponse error = objectMapper.readValue(body, EcuabetErrorResponse.class);
            if (error.getMessage() != null && !error.getMessage().isBlank()) {
                return "ECUABET reverso de deposito respondio con error: " + error.getMessage();
            }
            if (error.getError() != null && !error.getError().isBlank()) {
                return "ECUABET reverso de deposito respondio con error: " + error.getError();
            }
            return "ECUABET reverso de deposito respondio con error: " + objectMapper.writeValueAsString(error.getRaw());
        } catch (JsonProcessingException exception) {
            return "ECUABET reverso de deposito respondio con error no parseable";
        }
    }

    private void validateProviderConfiguration(AppProperties.ProviderProperties provider) {
        if (provider.getShopId() == null || provider.getShopId().isBlank()) {
            throw new IntegrationException("ECUABET requiere shop-id configurado");
        }
        if (provider.getCountry() == null) {
            throw new IntegrationException("ECUABET requiere country configurado");
        }
    }

    private java.math.BigDecimal requiredAmount(EcuabetDepositCommand command) {
        if (command.getAmount() == null) {
            throw new IntegrationException("ECUABET requiere amount para reverso de deposito");
        }
        return command.getAmount();
    }

    private Integer requiredTransactionId(EcuabetDepositCommand command) {
        if (command.getTransactionId() == null) {
            throw new IntegrationException("ECUABET requiere transactionId para reverso de deposito");
        }
        return command.getTransactionId();
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(PROVIDER_KEY);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor ECUABET");
        }
        return provider;
    }

    private void traceToConsole(String label, String url, String body) {
        log.info("{} url={} body={}", label, url, body);
    }

    private void traceErrorToConsole(String label, String url, String body) {
        log.error("{} url={} body={}", label, url, body);
    }
}

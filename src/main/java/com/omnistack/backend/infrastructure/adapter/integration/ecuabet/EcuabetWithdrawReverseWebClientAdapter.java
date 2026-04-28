package com.omnistack.backend.infrastructure.adapter.integration.ecuabet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.EcuabetWithdrawReversePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.EcuabetWithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetErrorResponse;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetWithdrawResponse;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetWithdrawReverseRequest;
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
import reactor.core.publisher.Mono;

/**
 * Adapter HTTP para el reverso de nota de retiro ECUABET.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcuabetWithdrawReverseWebClientAdapter implements EcuabetWithdrawReversePort {

    private static final String PROVIDER_KEY = "ecuabet";

    private final WebClient omnistackWebClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ProviderTokenResolverUseCase providerTokenResolverUseCase;

    /**
     * Ejecuta el consumo externo de reverso de nota de retiro ECUABET.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    @Override
    public ExternalTransactionResponse reverseWithdraw(EcuabetWithdrawCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        EcuabetWithdrawReverseRequest request = buildExternalRequest(command, provider);
        String url = resolveUrl(provider.getBaseUrl(), operationPath);

        traceToConsole("ECUABET withdraw reverse request", url, JsonUtil.toJsonSilently(request));

        EcuabetWithdrawResponse response = omnistackWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> addHeaders(headers, command))
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            traceErrorToConsole("ECUABET withdraw reverse error", url, body);
                            return Mono.error(new IntegrationException(buildErrorMessage(body)));
                        }))
                .bodyToMono(EcuabetWithdrawResponse.class)
                .block();

        if (response == null) {
            throw new IntegrationException("ECUABET no retorno contenido para reverso de nota de retiro");
        }

        traceToConsole("ECUABET withdraw reverse response", url, JsonUtil.toJsonSilently(response));

        return ExternalTransactionResponse.builder()
                .approved(response.getError() == null || response.getError() == 0)
                .externalCode(resolveExternalCode(response))
                .externalMessage(resolveExternalMessage(response))
                .payload(buildPayload(command, response))
                .build();
    }

    private void addHeaders(HttpHeaders headers, EcuabetWithdrawCommand command) {
        headers.add("chain", command.getChain());
        headers.add("store", command.getStore());
        if (command.getStoreName() != null) {
            headers.add("store_name", command.getStoreName());
        }
        headers.add("pos", command.getPos());
        headers.add("channel_POS", command.getChannelPos().name());
    }

    private EcuabetWithdrawReverseRequest buildExternalRequest(
            EcuabetWithdrawCommand command,
            AppProperties.ProviderProperties provider) {
        validateProviderConfiguration(provider);
        String providerToken = providerTokenResolverUseCase.getToken(
                command.getCategoryCode(),
                command.getSubcategoryCode(),
                provider.getServiceProviderCode());

        return EcuabetWithdrawReverseRequest.builder()
                .shop(provider.getShopId())
                .token(providerToken)
                .country(provider.getCountry())
                .withdrawId(requiredValue(command.getWithdrawId(), "withdrawId"))
                .password(requiredValue(command.getPassword(), "password"))
                .transactionId(requiredTransactionId(command))
                .build();
    }

    private Map<String, Object> buildPayload(EcuabetWithdrawCommand command, EcuabetWithdrawResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", response.getError());
        payload.put("code", resolveExternalCode(response));
        payload.put("message", resolveExternalMessage(response));
        payload.put("authorization", String.valueOf(command.getTransactionId()));
        payload.put("document", command.getDocument());
        payload.put("amount", command.getAmount());
        payload.put("withdrawId", command.getWithdrawId());
        payload.put("providerTransactionId", response.getTransactionId());
        payload.putAll(response.getRaw());
        return payload;
    }

    private String resolveExternalCode(EcuabetWithdrawResponse response) {
        return response.getCode() != null && !response.getCode().isBlank() ? response.getCode() : "0";
    }

    private String resolveExternalMessage(EcuabetWithdrawResponse response) {
        if (response.getMessage() != null && !response.getMessage().isBlank()) {
            return response.getMessage();
        }
        return response.getError() != null && response.getError() != 0
                ? String.valueOf(response.getError())
                : "Transaccion correcta";
    }

    private String buildErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Error HTTP al invocar reverso de nota de retiro ECUABET";
        }

        try {
            EcuabetErrorResponse error = objectMapper.readValue(body, EcuabetErrorResponse.class);
            if (error.getMessage() != null && !error.getMessage().isBlank()) {
                return "ECUABET reverso de nota de retiro respondio con error: " + error.getMessage();
            }
            if (error.getError() != null && !error.getError().isBlank()) {
                return "ECUABET reverso de nota de retiro respondio con error: " + error.getError();
            }
            return "ECUABET reverso de nota de retiro respondio con error: " + objectMapper.writeValueAsString(error.getRaw());
        } catch (JsonProcessingException exception) {
            return "ECUABET reverso de nota de retiro respondio con error no parseable";
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

    private Integer requiredTransactionId(EcuabetWithdrawCommand command) {
        if (command.getTransactionId() == null) {
            throw new IntegrationException("ECUABET requiere transactionId para reverso de nota de retiro");
        }
        return command.getTransactionId();
    }

    private String requiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IntegrationException("ECUABET requiere el campo " + fieldName + " para reverso de nota de retiro");
        }
        return value;
    }

    private String resolveUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        Map<String, AppProperties.ProviderProperties> providers = appProperties.getIntegration().getProviders();
        AppProperties.ProviderProperties provider = providers.get(PROVIDER_KEY);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor ECUABET");
        }
        return provider;
    }

    private void traceToConsole(String label, String url, String body) {
        log.info("{} url={} body={}", label, url, body);
        System.out.println(label + " url=" + url + " body=" + body);
    }

    private void traceErrorToConsole(String label, String url, String body) {
        log.error("{} url={} body={}", label, url, body);
        System.err.println(label + " url=" + url + " body=" + body);
    }
}

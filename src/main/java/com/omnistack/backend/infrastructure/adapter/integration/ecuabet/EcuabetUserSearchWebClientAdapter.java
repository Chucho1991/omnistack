package com.omnistack.backend.infrastructure.adapter.integration.ecuabet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.out.EcuabetUserSearchPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.EcuabetUserSearchCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetErrorResponse;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetUserSearchRequest;
import com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto.EcuabetUserSearchResponse;
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
 * Adapter HTTP para la operacion Buscar usuario de ECUABET.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcuabetUserSearchWebClientAdapter implements EcuabetUserSearchPort {

    private static final String PROVIDER_KEY = "ecuabet";

    private final WebClient omnistackWebClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    public ExternalTransactionResponse searchUser(EcuabetUserSearchCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        EcuabetUserSearchRequest request = buildExternalRequest(command, provider);
        String url = provider.getBaseUrl() + operationPath;

        traceToConsole("External web service request", url, JsonUtil.toJsonSilently(request));

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
                            return Mono.error(new IntegrationException(buildErrorMessage(body)));
                        }))
                .bodyToMono(EcuabetUserSearchResponse.class)
                .block();

        if (response == null) {
            throw new IntegrationException("ECUABET no retorno contenido para la operacion user/search");
        }

        traceToConsole("External web service response", url, JsonUtil.toJsonSilently(response));

        return ExternalTransactionResponse.builder()
                .approved(response.getError() == null || response.getError() == 0)
                .externalCode(resolveExternalCode(response))
                .externalMessage(resolveExternalMessage(response))
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
        return EcuabetUserSearchRequest.builder()
                .shop(provider.getShopId())
                .token(provider.getToken())
                .userid(nullIfBlank(command.getUserid()))
                .country(provider.getCountry())
                .phone(nullIfBlank(command.getPhone()))
                .document(nullIfBlank(command.getDocument()))
                .build();
    }

    private String resolveExternalCode(EcuabetUserSearchResponse response) {
        return response.getCode() != null && !response.getCode().isBlank() ? response.getCode() : "00";
    }

    private String resolveExternalMessage(EcuabetUserSearchResponse response) {
        if (response.getError() != null && response.getError() != 0) {
            return "Error reportado por ECUABET";
        }
        return response.getMessage() != null && !response.getMessage().isBlank()
                ? response.getMessage()
                : "Operacion procesada por ECUABET";
    }

    private String buildErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Error HTTP al invocar ECUABET user/search";
        }

        try {
            EcuabetErrorResponse error = objectMapper.readValue(body, EcuabetErrorResponse.class);
            if (error.getMessage() != null && !error.getMessage().isBlank()) {
                return "ECUABET user/search respondio con error: " + error.getMessage();
            }
            if (error.getError() != null && !error.getError().isBlank()) {
                return "ECUABET user/search respondio con error: " + error.getError();
            }
            return "ECUABET user/search respondio con error: " + objectMapper.writeValueAsString(error.getRaw());
        } catch (JsonProcessingException exception) {
            return "ECUABET user/search respondio con error no parseable";
        }
    }

    private Map<String, Object> buildPayload(EcuabetUserSearchResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", response.getCode());
        payload.put("error", response.getError());
        payload.put("name", response.getName());
        payload.put("userid", response.getUserid());
        payload.putAll(response.getRaw());
        return payload;
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        Map<String, AppProperties.ProviderProperties> providers = appProperties.getIntegration().getProviders();
        AppProperties.ProviderProperties provider = providers.get(PROVIDER_KEY);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor ECUABET");
        }
        return provider;
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
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

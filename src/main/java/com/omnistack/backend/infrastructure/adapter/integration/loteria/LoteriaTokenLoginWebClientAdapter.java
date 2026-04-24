package com.omnistack.backend.infrastructure.adapter.integration.loteria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.out.ProviderTokenLoginPort;
import com.omnistack.backend.domain.model.ProviderTokenLoginCommand;
import com.omnistack.backend.domain.model.ProviderTokenLoginResult;
import com.omnistack.backend.infrastructure.adapter.integration.loteria.dto.LoteriaLoginRequest;
import com.omnistack.backend.infrastructure.adapter.integration.loteria.dto.LoteriaLoginResponse;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adapter HTTP para autenticacion de integraciones de Loteria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoteriaTokenLoginWebClientAdapter implements ProviderTokenLoginPort {

    private final WebClient omnistackWebClient;
    private final ObjectMapper objectMapper;

    /**
     * Solicita un token dinamico al endpoint de login configurado.
     *
     * @param command datos de autenticacion del proveedor
     * @return token vigente emitido por el proveedor
     */
    @Override
    public ProviderTokenLoginResult login(ProviderTokenLoginCommand command) {
        LoteriaLoginRequest request = LoteriaLoginRequest.builder()
                .username(command.getUsername())
                .password(command.getPassword())
                .productToSell(command.getProductToSell())
                .build();
        String url = resolveUrl(command.getBaseUrl(), command.getPath());

        traceToConsole("Loteria token login request", url, JsonUtil.toJsonSilently(request));

        LoteriaLoginResponse response = omnistackWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            traceErrorToConsole("Loteria token login error", url, body);
                            return Mono.error(new IntegrationException(buildErrorMessage(command.getProviderName(), body)));
                        }))
                .bodyToMono(String.class)
                .map(body -> parseResponseBody(command.getProviderName(), body))
                .block();

        if (response == null) {
            throw new IntegrationException("Loteria no retorno contenido para el login del proveedor " + command.getProviderName());
        }

        traceToConsole("Loteria token login response", url, JsonUtil.toJsonSilently(response));

        if (response.getErrorCode() != null && response.getErrorCode() != 0) {
            throw new IntegrationException("Loteria login respondio con error para " + command.getProviderName()
                    + ": " + defaultMessage(response.getErrorMessage(), "Error desconocido"));
        }
        if (response.getToken() == null || response.getToken().isBlank()) {
            throw new IntegrationException("Loteria login no retorno token para el proveedor " + command.getProviderName());
        }

        return ProviderTokenLoginResult.builder()
                .token(response.getToken())
                .build();
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

    private String buildErrorMessage(String providerName, String body) {
        if (body == null || body.isBlank()) {
            return "Error HTTP al invocar login de Loteria para " + providerName;
        }
        try {
            LoteriaLoginResponse response = objectMapper.readValue(body, LoteriaLoginResponse.class);
            return "Loteria login respondio con error para " + providerName + ": "
                    + defaultMessage(response.getErrorMessage(), JsonUtil.toJsonSilently(response.getRaw()));
        } catch (JsonProcessingException exception) {
            return "Loteria login respondio con error no parseable para " + providerName;
        }
    }

    private LoteriaLoginResponse parseResponseBody(String providerName, String body) {
        if (body == null || body.isBlank()) {
            throw new IntegrationException("Loteria no retorno contenido para el login del proveedor " + providerName);
        }
        try {
            return objectMapper.readValue(body, LoteriaLoginResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IntegrationException("Loteria login retorno un body no parseable para " + providerName);
        }
    }

    private String defaultMessage(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
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

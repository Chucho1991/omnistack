package com.omnistack.backend.infrastructure.adapter.integration.loteria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.Bet593RechargePort;
import com.omnistack.backend.application.port.out.Bet593RechargeValidationPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.Bet593RechargeCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.infrastructure.adapter.integration.loteria.dto.Bet593RechargeRequest;
import com.omnistack.backend.infrastructure.adapter.integration.loteria.dto.Bet593RechargeResponse;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Adapter HTTP para la recarga de saldo BET593 de Loteria Nacional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Bet593RechargeWebClientAdapter implements Bet593RechargePort, Bet593RechargeValidationPort {

    private static final String PROVIDER_KEY = "loteria";

    private final WebClient omnistackWebClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ProviderTokenResolverUseCase providerTokenResolverUseCase;

    /**
     * Ejecuta el consumo externo de recarga BET593.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    @Override
    public ExternalTransactionResponse recharge(Bet593RechargeCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        Bet593RechargeRequest request = buildExternalRequest(command, provider);
        return invokeLoteria(operationPath, provider, request, "recharge", "recarga BET593");
    }

    /**
     * Ejecuta el consumo externo de validacion de recarga BET593.
     *
     * @param command request interno normalizado para la validacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    @Override
    public ExternalTransactionResponse validateRecharge(Bet593RechargeCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        Bet593RechargeRequest request = buildExternalValidationRequest(command, provider);
        return invokeLoteria(operationPath, provider, request, "validate recharge", "validacion de recarga BET593");
    }

    private ExternalTransactionResponse invokeLoteria(
            String operationPath,
            AppProperties.ProviderProperties provider,
            Bet593RechargeRequest request,
            String logOperation,
            String errorOperation) {
        String url = resolveUrl(provider.getBaseUrl(), operationPath);

        traceToConsole("Loteria BET593 " + logOperation + " request", url, JsonUtil.toJsonSilently(request));

        Bet593RechargeResponse response;
        try {
            response = omnistackWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                traceErrorToConsole("Loteria BET593 " + logOperation + " error", url, body);
                                return Mono.error(new IntegrationException(buildErrorMessage(body, errorOperation)));
                            }))
                    .bodyToMono(String.class)
                    .map(this::parseResponseBody)
                    .block();
        } catch (WebClientRequestException exception) {
            traceErrorToConsole("Loteria BET593 " + logOperation + " transport error", url, rootCauseMessage(exception));
            throw new IntegrationException(buildTransportErrorMessage(url, exception, errorOperation), exception);
        }

        if (response == null) {
            throw new IntegrationException("Loteria no retorno contenido para " + errorOperation);
        }

        traceToConsole("Loteria BET593 " + logOperation + " response", url, JsonUtil.toJsonSilently(response));

        return ExternalTransactionResponse.builder()
                .approved(!hasBusinessError(response))
                .externalCode(resolveExternalCode(response))
                .externalMessage(resolveExternalMessage(response))
                .payload(buildPayload(response))
                .build();
    }

    private Bet593RechargeRequest buildExternalRequest(
            Bet593RechargeCommand command,
            AppProperties.ProviderProperties provider) {
        validateProviderConfiguration(provider);
        String providerToken = providerTokenResolverUseCase.getToken(
                command.getCategoryCode(),
                command.getSubcategoryCode(),
                provider.getServiceProviderCode());

        return Bet593RechargeRequest.builder()
                .usuario(provider.getAuth().getLogin().getUsername())
                .token(providerToken)
                .canal(provider.getCanal())
                .medioId(provider.getMedioId())
                .puntooperacionId(provider.getPuntoOperacionId())
                .cuentaweb(requiredValue(command.getDocument(), "document"))
                .recargaid(command.getAuthorization())
                .serialnumber(command.getSerialnumber())
                .valor(formatAmount(command.getAmount()))
                .codigotrn(requiredValue(command.getUuid(), "uuid"))
                .build();
    }

    private Bet593RechargeRequest buildExternalValidationRequest(
            Bet593RechargeCommand command,
            AppProperties.ProviderProperties provider) {
        validateProviderConfiguration(provider);
        String providerToken = providerTokenResolverUseCase.getToken(
                command.getCategoryCode(),
                command.getSubcategoryCode(),
                provider.getServiceProviderCode());

        return Bet593RechargeRequest.builder()
                .usuario(provider.getAuth().getLogin().getUsername())
                .token(providerToken)
                .canal(provider.getCanal())
                .medioId(provider.getMedioId())
                .puntooperacionId(provider.getPuntoOperacionId())
                .cuentaweb(requiredValue(command.getDocument(), "document"))
                .recargaid(requiredValue(command.getAuthorization(), "authorization"))
                .serialnumber(requiredValue(command.getSerialnumber(), "serialnumber"))
                .build();
    }

    private Map<String, Object> buildPayload(Bet593RechargeResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", hasBusinessError(response) ? 1 : 0);
        payload.put("code", resolveExternalCode(response));
        payload.put("message", resolveExternalMessage(response));
        payload.put("name", response.getNombre());
        payload.put("lastname", response.getApellido());
        payload.put("authorization", response.getRecargaid());
        payload.put("serialnumber", response.getSerialnumber());
        payload.put("userid", null);
        payload.put("document", response.getCuentaweb());
        payload.put("amount", response.getValor());
        payload.put("status", response.getEstado());
        payload.putAll(response.getRaw());
        return payload;
    }

    private boolean hasBusinessError(Bet593RechargeResponse response) {
        return response.getMsgError() != null && !response.getMsgError().isBlank();
    }

    private String resolveExternalCode(Bet593RechargeResponse response) {
        return response.getCodError() == null ? "0" : String.valueOf(response.getCodError());
    }

    private String resolveExternalMessage(Bet593RechargeResponse response) {
        return response.getMsgError() != null ? response.getMsgError() : "";
    }

    private String buildErrorMessage(String body, String operation) {
        if (body == null || body.isBlank()) {
            return "Error HTTP al invocar " + operation + " de Loteria";
        }
        try {
            Bet593RechargeResponse response = objectMapper.readValue(body, Bet593RechargeResponse.class);
            return "Loteria " + operation + " respondio con error: "
                    + defaultMessage(response.getMsgError(), JsonUtil.toJsonSilently(response.getRaw()));
        } catch (JsonProcessingException exception) {
            return "Loteria " + operation + " respondio con error no parseable";
        }
    }

    private String buildTransportErrorMessage(String url, WebClientRequestException exception, String operation) {
        if (hasCause(exception, "ReadTimeoutException")) {
            return "Timeout al invocar " + operation + " de Loteria: " + url;
        }
        if (hasCause(exception, "SSLHandshakeException") || hasCause(exception, "SunCertPathBuilderException")) {
            return "Error SSL al invocar " + operation
                    + " de Loteria. Revise el certificado/truststore del contenedor para " + url;
        }
        return "Error de conexion al invocar " + operation + " de Loteria: " + rootCauseMessage(exception);
    }

    private boolean hasCause(Throwable exception, String simpleClassName) {
        Throwable current = exception;
        while (current != null) {
            if (current.getClass().getSimpleName().equals(simpleClassName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String rootCauseMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : current.getClass().getSimpleName() + ": " + message;
    }

    private Bet593RechargeResponse parseResponseBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IntegrationException("Loteria no retorno contenido para recarga BET593");
        }
        try {
            return objectMapper.readValue(body, Bet593RechargeResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IntegrationException("Loteria recarga BET593 retorno un body no parseable");
        }
    }

    private void validateProviderConfiguration(AppProperties.ProviderProperties provider) {
        if (provider.getAuth() == null || provider.getAuth().getLogin() == null
                || provider.getAuth().getLogin().getUsername() == null
                || provider.getAuth().getLogin().getUsername().isBlank()) {
            throw new IntegrationException("Loteria BET593 requiere auth.login.username configurado");
        }
        if (provider.getCanal() == null || provider.getCanal().isBlank()) {
            throw new IntegrationException("Loteria BET593 requiere canal configurado");
        }
        if (provider.getMedioId() == null) {
            throw new IntegrationException("Loteria BET593 requiere medioId configurado");
        }
        if (provider.getPuntoOperacionId() == null) {
            throw new IntegrationException("Loteria BET593 requiere puntoOperacionId configurado");
        }
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
            throw new IntegrationException("No existe configuracion para el proveedor Loteria");
        }
        return provider;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IntegrationException("Loteria BET593 requiere el campo amount");
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private String requiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IntegrationException("Loteria BET593 requiere el campo " + fieldName);
        }
        return value;
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

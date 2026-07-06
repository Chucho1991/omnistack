package com.omnistack.backend.infrastructure.adapter.integration.loteria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.Bet593WithdrawPort;
import com.omnistack.backend.application.port.out.Bet593WithdrawReversePort;
import com.omnistack.backend.application.port.out.Bet593WithdrawValidationPort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.WsExtLogService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.Bet593WithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ProviderCallLog;
import com.omnistack.backend.infrastructure.adapter.integration.loteria.dto.Bet593WithdrawRequest;
import com.omnistack.backend.infrastructure.adapter.integration.loteria.dto.Bet593WithdrawResponse;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Adapter HTTP para la nota de retiro BET593 de Loteria Nacional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Bet593WithdrawWebClientAdapter implements Bet593WithdrawPort, Bet593WithdrawValidationPort, Bet593WithdrawReversePort {

    private static final String PROVIDER_KEY = "loteria";
    private static final String EXECUTE_OPERATION = "EXECUTE";
    private static final String VERIFY_OPERATION = "VERIFY";
    private static final String REVERSE_OPERATION = "REVERSE";
    private static final String WS_KEY_EXECUTE = "EXECUTE.CASHOUT";
    private static final String WS_KEY_VERIFY = "VERIFY.CASHOUT";
    private static final String WS_KEY_REVERSE = "REVERSE.CASHOUT";

    private final WebClient omnistackWebClient;
    private final ProviderConfigService providerConfigService;
    private final ObjectMapper objectMapper;
    private final ProviderTokenResolverUseCase providerTokenResolverUseCase;
    private final WsExtLogService wsExtLogService;

    /**
     * Ejecuta el consumo externo de nota de retiro BET593.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    @Override
    public ExternalTransactionResponse withdraw(Bet593WithdrawCommand command, String operationPath) {
        return consumeWithdraw(command, operationPath, EXECUTE_OPERATION, "Loteria BET593 withdraw", WS_KEY_EXECUTE);
    }

    /**
     * Ejecuta el consumo externo de validacion de nota de retiro BET593.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    @Override
    public ExternalTransactionResponse validateWithdraw(Bet593WithdrawCommand command, String operationPath) {
        return consumeWithdraw(command, operationPath, VERIFY_OPERATION, "Loteria BET593 withdraw verify", WS_KEY_VERIFY);
    }

    /**
     * Ejecuta el consumo externo de reverso de nota de retiro BET593.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    @Override
    public ExternalTransactionResponse reverseWithdraw(Bet593WithdrawCommand command, String operationPath) {
        return consumeWithdraw(command, operationPath, REVERSE_OPERATION, "Loteria BET593 withdraw reverse", WS_KEY_REVERSE);
    }

    private ExternalTransactionResponse consumeWithdraw(
            Bet593WithdrawCommand command,
            String operationPath,
            String operationKey,
            String traceLabel,
            String wsKey) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        Bet593WithdrawRequest request = buildExternalRequest(command, provider, operationKey);
        String url = operationPath;
        Bet593WithdrawResponse response = executeWithdrawRequest(request, url, traceLabel, command.getUuid(), wsKey);

        if (isInvalidTokenResponse(response)) {
            traceToConsole(traceLabel + " token refresh", url,
                    "Token invalido detectado; regenerando token y reintentando");
            providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            request = buildExternalRequest(command, provider, operationKey);
            response = executeWithdrawRequest(request, url, traceLabel + " retry", command.getUuid(), wsKey);
        }

        if (response == null) {
            throw new IntegrationException("Loteria no retorno contenido para nota de retiro BET593");
        }
        applyRequestFallbacks(response, request);

        traceToConsole(traceLabel + " response", url, JsonUtil.toJsonSilently(response));

        return ExternalTransactionResponse.builder()
                .approved(!hasBusinessError(response))
                .externalCode(resolveExternalCode(response))
                .externalMessage(resolveExternalMessage(response))
                .payload(buildPayload(response, request))
                .build();
    }

    private Bet593WithdrawResponse executeWithdrawRequest(
            Bet593WithdrawRequest request,
            String url,
            String traceLabel,
            String uuid,
            String wsKey) {
        long startMs = System.currentTimeMillis();
        String requestJson = JsonUtil.toJsonSilently(request);
        traceToConsole(traceLabel + " request", url, requestJson);

        Bet593WithdrawResponse response;
        try {
            response = omnistackWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                traceErrorToConsole(traceLabel + " error", url, body);
                                wsExtLogService.log(ProviderCallLog.builder()
                                        .uuid(uuid)
                                        .providerKey(PROVIDER_KEY)
                                        .wsKey(wsKey)
                                        .url(url)
                                        .requestJson(requestJson)
                                        .responseJson(body)
                                        .durationMs(System.currentTimeMillis() - startMs)
                                        .isError(true)
                                        .errorMessage(buildErrorMessage(body))
                                        .build());
                                return Mono.error(new IntegrationException(buildErrorMessage(body)));
                            }))
                    .bodyToMono(String.class)
                    .map(this::parseResponseBody)
                    .block();
        } catch (WebClientRequestException exception) {
            traceErrorToConsole(traceLabel + " transport error", url, rootCauseMessage(exception));
            wsExtLogService.log(ProviderCallLog.builder()
                    .uuid(uuid)
                    .providerKey(PROVIDER_KEY)
                    .wsKey(wsKey)
                    .url(url)
                    .requestJson(requestJson)
                    .responseJson(null)
                    .durationMs(System.currentTimeMillis() - startMs)
                    .isError(true)
                    .errorMessage(buildTransportErrorMessage(url, exception))
                    .build());
            throw new IntegrationException(buildTransportErrorMessage(url, exception), exception);
        }
        if (response != null) {
            wsExtLogService.log(ProviderCallLog.builder()
                    .uuid(uuid)
                    .providerKey(PROVIDER_KEY)
                    .wsKey(wsKey)
                    .url(url)
                    .requestJson(requestJson)
                    .responseJson(JsonUtil.toJsonSilently(response))
                    .durationMs(System.currentTimeMillis() - startMs)
                    .isError(false)
                    .build());
        }
        return response;
    }

    private Bet593WithdrawRequest buildExternalRequest(
            Bet593WithdrawCommand command,
            AppProperties.ProviderProperties provider,
            String operationKey) {
        validateProviderConfiguration(provider);
        String providerToken = providerTokenResolverUseCase.getToken(PROVIDER_KEY);
        String username = provider.getAuth().getLogin().getUsername();

        return Bet593WithdrawRequest.builder()
                .usuario(username)
                .maquina(provider.getShopIp())
                .operacion(resolveOperacionName(operationKey))
                .token(providerToken)
                .usuarioId(username)
                .clienteId(provider.getClienteId())
                .medioId(provider.getMedioId())
                .numeroTransaccion(resolveTransactionNumber(command, operationKey))
                .identificacion(requiredValue(command.getDocument(), "document"))
                .numeroRetiro(resolveWithdrawNumber(command, operationKey))
                .motivo(resolveMotivo(command, operationKey))
                .build();
    }

    private String resolveProviderToken(
            Bet593WithdrawCommand command,
            AppProperties.ProviderProperties provider,
            boolean forceRefreshToken) {
        if (forceRefreshToken) {
            return providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
        }
        return providerTokenResolverUseCase.getToken(PROVIDER_KEY);
    }

    private String resolveTransactionNumber(Bet593WithdrawCommand command, String operationKey) {
        if (REVERSE_OPERATION.equals(operationKey)) {
            return requiredValue(command.getAuthorization(), "authorization");
        }
        return requiredValue(command.getUuid(), "uuid");
    }

    private String resolveWithdrawNumber(Bet593WithdrawCommand command, String operationKey) {
        if (REVERSE_OPERATION.equals(operationKey)) {
            return null;
        }
        return requiredValue(command.getWithdrawId(), "withdrawId");
    }

    private String resolveMotivo(Bet593WithdrawCommand command, String operationKey) {
        if (REVERSE_OPERATION.equals(operationKey)) {
            return requiredValue(command.getMotivo(), "motivo");
        }
        return null;
    }

    private void applyRequestFallbacks(Bet593WithdrawResponse response, Bet593WithdrawRequest request) {
        if (response.getNumeroTransaccion() == null || response.getNumeroTransaccion().isBlank()) {
            response.setNumeroTransaccion(request.getNumeroTransaccion());
        }
    }

    private Map<String, Object> buildPayload(Bet593WithdrawResponse response, Bet593WithdrawRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", hasBusinessError(response) ? 1 : 0);
        payload.put("code", resolveExternalCode(response));
        payload.put("message", resolveExternalMessage(response));
        payload.put("name", response.getNombre());
        payload.put("lastname", null);
        payload.put("currency", null);
        payload.put("authorization", response.getOrdenPagoId());
        payload.put("serialnumber", null);
        payload.put("userid", null);
        payload.put("document", response.getIdentificacion());
        payload.put("amount", response.getValor());
        payload.put("transactionNumber", defaultMessage(response.getNumeroTransaccion(), request.getNumeroTransaccion()));
        payload.put("date", response.getFecha());
        payload.putAll(response.getRaw());
        return payload;
    }

    private boolean hasBusinessError(Bet593WithdrawResponse response) {
        return response.getMsgError() != null && !response.getMsgError().isBlank();
    }

    private String resolveExternalCode(Bet593WithdrawResponse response) {
        return response.getCodError() == null ? "0" : String.valueOf(response.getCodError());
    }

    private String resolveExternalMessage(Bet593WithdrawResponse response) {
        return response.getMsgError() != null ? response.getMsgError() : "";
    }

    private boolean isInvalidTokenResponse(Bet593WithdrawResponse response) {
        if (response == null || response.getMsgError() == null) {
            return false;
        }
        return isInvalidTokenMessage(response.getMsgError());
    }

    private boolean isInvalidTokenException(IntegrationException exception) {
        return exception.getMessage() != null && isInvalidTokenMessage(exception.getMessage());
    }

    private boolean isInvalidTokenMessage(String value) {
        String message = value.toLowerCase(Locale.ROOT);
        return message.contains("token")
                && (message.contains("invalid")
                || message.contains("inval")
                || message.contains("expir")
                || message.contains("venc")
                || message.contains("caduc")
                || message.contains("autoriz")
                || message.contains("sesion"));
    }

    private String buildErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Error HTTP al invocar nota de retiro BET593 de Loteria";
        }
        try {
            Bet593WithdrawResponse response = objectMapper.readValue(body, Bet593WithdrawResponse.class);
            return "Loteria nota de retiro BET593 respondio con error: "
                    + defaultMessage(response.getMsgError(), JsonUtil.toJsonSilently(response.getRaw()));
        } catch (JsonProcessingException exception) {
            return "Loteria nota de retiro BET593 respondio con error no parseable";
        }
    }

    private String buildTransportErrorMessage(String url, WebClientRequestException exception) {
        if (hasCause(exception, "ReadTimeoutException")) {
            return "Timeout al invocar nota de retiro BET593 de Loteria: " + url;
        }
        if (hasCause(exception, "SSLHandshakeException") || hasCause(exception, "SunCertPathBuilderException")) {
            return "Error SSL al invocar nota de retiro BET593 de Loteria. Revise el certificado/truststore del contenedor para " + url;
        }
        return "Error de conexion al invocar nota de retiro BET593 de Loteria: " + rootCauseMessage(exception);
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

    private Bet593WithdrawResponse parseResponseBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IntegrationException("Loteria no retorno contenido para nota de retiro BET593");
        }
        try {
            return objectMapper.readValue(body, Bet593WithdrawResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IntegrationException("Loteria nota de retiro BET593 retorno un body no parseable");
        }
    }

    private void validateProviderConfiguration(AppProperties.ProviderProperties provider) {
        if (provider.getAuth() == null || provider.getAuth().getLogin() == null
                || provider.getAuth().getLogin().getUsername() == null
                || provider.getAuth().getLogin().getUsername().isBlank()) {
            throw new IntegrationException("Loteria BET593 requiere auth.login.username configurado");
        }
        if (provider.getShopIp() == null || provider.getShopIp().isBlank()) {
            throw new IntegrationException("Loteria BET593 requiere shop-ip configurado para maquina");
        }
        if (provider.getClienteId() == null) {
            throw new IntegrationException("Loteria BET593 requiere cliente-id configurado");
        }
        if (provider.getMedioId() == null) {
            throw new IntegrationException("Loteria BET593 requiere medioId configurado");
        }
    }

    private String resolveOperacionName(String operationKey) {
        return switch (operationKey) {
            case EXECUTE_OPERATION -> "RETIROOL";
            case VERIFY_OPERATION -> "CONRETIROOL";
            case REVERSE_OPERATION -> "REVRETIROOL";
            default -> throw new IntegrationException("Loteria BET593 operacion cashout desconocida: " + operationKey);
        };
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(PROVIDER_KEY);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor Loteria");
        }
        return provider;
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
    }

    private void traceErrorToConsole(String label, String url, String body) {
        log.error("{} url={} body={}", label, url, body);
    }
}

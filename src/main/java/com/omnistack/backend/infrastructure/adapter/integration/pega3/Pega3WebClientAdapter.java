package com.omnistack.backend.infrastructure.adapter.integration.pega3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.Pega3CancelTicketPort;
import com.omnistack.backend.application.port.out.Pega3ComprobanteQueryPort;
import com.omnistack.backend.application.port.out.Pega3CreateTicketPort;
import com.omnistack.backend.application.port.out.Pega3DrawQueryPort;
import com.omnistack.backend.application.port.out.Pega3PayTicketPort;
import com.omnistack.backend.application.port.out.Pega3ProductQueryPort;
import com.omnistack.backend.application.port.out.Pega3VerifyTicketPort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.WsExtLogService;
import com.omnistack.backend.config.ProviderCircuitBreaker;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ProviderCallLog;
import com.omnistack.backend.domain.model.Pega3CancelTicketCommand;
import com.omnistack.backend.domain.model.Pega3ComprobanteQueryCommand;
import com.omnistack.backend.domain.model.Pega3CreateTicketCommand;
import com.omnistack.backend.domain.model.Pega3DrawQueryCommand;
import com.omnistack.backend.domain.model.Pega3Panel;
import com.omnistack.backend.domain.model.Pega3PayTicketCommand;
import com.omnistack.backend.domain.model.Pega3ProductQueryCommand;
import com.omnistack.backend.domain.model.Pega3VerifyTicketCommand;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3CancelTicketRequest;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3CancelTicketResponse;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3ComprobanteResponse;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3CreateTicketRequest;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3CreateTicketResponse;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3DrawQueryRequest;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3DrawQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3PayTicketRequest;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3PayTicketResponse;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3ProductQueryRequest;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3ProductQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3VerifyTicketRequest;
import com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3VerifyTicketResponse;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Adapter HTTP para todas las operaciones del proveedor Pega3 de Loteria Nacional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Pega3WebClientAdapter implements
        Pega3ProductQueryPort,
        Pega3DrawQueryPort,
        Pega3CreateTicketPort,
        Pega3PayTicketPort,
        Pega3VerifyTicketPort,
        Pega3ComprobanteQueryPort,
        Pega3CancelTicketPort {

    private static final String PROVIDER_KEY = "pega3";
    private static final String GAME_CODE = "1001";
    private static final int ADVANCE_DRAW = 0;
    private static final int NO_OF_DRAWS = 1;
    private static final int ENTRY_TYPE_REGULAR = 1;
    private static final int TYPE_OF_ENTRY_QUICK_PICK = 1;
    private static final int TYPE_OF_ENTRY_MANUAL = 3;
    private static final String WS_KEY_PRECHECK = "PRECHECK.CASHIN";
    private static final String WS_KEY_PRECHECK_SORTEO = "PRECHECK_SORTEO.CASHIN";
    private static final String WS_KEY_CREATE_TICKET = "CREATE_TICKET.CASHIN";
    private static final String WS_KEY_VERIFY_COMPROBANTE = "VERIFY_COMPROBANTE.CASHIN";
    private static final String WS_KEY_REVERSE = "REVERSE.CASHIN";

    private final WebClient omnistackWebClient;
    private final ProviderConfigService providerConfigService;
    private final ObjectMapper objectMapper;
    private final ProviderTokenResolverUseCase providerTokenResolverUseCase;
    private final WsExtLogService wsExtLogService;
    private final ProviderCircuitBreaker providerCircuitBreaker;

    @Override
    public ExternalTransactionResponse queryProduct(Pega3ProductQueryCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();
        String productoVender = resolveProductoVender(provider);

        Pega3ProductQueryRequest request = Pega3ProductQueryRequest.builder()
                .token(token)
                .productoVender(productoVender)
                .build();

        Pega3ProductQueryResponse response = invokePega3(
                operationPath, provider, request, Pega3ProductQueryResponse.class,
                "queryProduct", "consulta producto Pega3", command.getUuid(), WS_KEY_PRECHECK);

        // El proveedor trae 4 entryTypes (Playslip-Manual/QuickPick, Verbal-Manual/QuickPick) —
        // "Playslip" es el canal de papeleta fisica de autoservicio, que GEOPos no tiene (confirmado
        // contra docs/GPFEC-3477 Recaudo Loteria - Negocio.pdf, RF-07/CU-04: el cajero solo digita
        // numeros o pide "numero aleatorio", nunca papeleta). Se filtra a solo "Verbal-*" antes de
        // exponer al POS, para no ofrecer una opcion que no corresponde a ningun flujo real.
        List<Pega3ProductQueryResponse.EntryType> verbalEntryTypes = response.getEntryTypes() == null
                ? List.of()
                : response.getEntryTypes().stream()
                        .filter(et -> et.getCode() != null && et.getCode().startsWith("Verbal-"))
                        .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = hasError(response.getMessage());
        payload.put("message", response.getMessage());
        payload.put("entry_types", verbalEntryTypes.stream()
                .map(Pega3ProductQueryResponse.EntryType::getCode)
                .toList());
        payload.put("bet_amount_options", parseBetAmountOptions(response.getBetAmountOptions()));
        payload.put("min_cost", response.getMinCost());
        payload.put("retailer_cancel_period", response.getRetailerCancelPeriod());
        payload.put("prize_liability_threshold", response.getPrizeLiabilityThreshold());

        // Los limites (maxWager, futureDrawsLimit, advanceDrawLimit, playTypes) vienen anidados
        // por modalidad de entrada (entryTypes[]); se toma el primero de los Verbal-* como
        // representativo, igual que ya se hacia con minCost a nivel plano.
        Pega3ProductQueryResponse.EntryType firstEntryType = verbalEntryTypes.isEmpty() ? null : verbalEntryTypes.get(0);
        if (firstEntryType != null) {
            payload.put("max_cost", firstEntryType.getMaxWager());
            payload.put("future_draws_limit", firstEntryType.getFutureDrawsLimit());
            payload.put("advance_draw_limit", firstEntryType.getAdvanceDrawLimit());
            payload.put("play_types", firstEntryType.getPlayTypes());
        }

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(isError ? "ERROR" : "0")
                .externalMessage(response.getMessage() != null ? response.getMessage() : "")
                .payload(payload)
                .build();
    }

    private List<BigDecimal> parseBetAmountOptions(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(BigDecimal::new)
                .toList();
    }

    @Override
    public ExternalTransactionResponse queryActiveDraw(Pega3DrawQueryCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();
        String productoVender = resolveProductoVender(provider);
        String deviceId = requiredValue(provider.getAuth().getLogin().getUsername(), "auth.login.username");

        Pega3DrawQueryRequest request = Pega3DrawQueryRequest.builder()
                .deviceId(deviceId)
                .token(token)
                .productoVender(productoVender)
                .build();

        Pega3DrawQueryResponse response = invokePega3(
                operationPath, provider, request, Pega3DrawQueryResponse.class,
                "queryActiveDraw", "consulta sorteo activo Pega3", command.getUuid(), WS_KEY_PRECHECK_SORTEO);

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = hasError(response.getMessage());
        payload.put("message", response.getMessage());
        payload.put("draw_number", response.getDrawNumber());
        payload.put("draw_date", response.getDrawDate());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(isError ? "ERROR" : "0")
                .externalMessage(response.getMessage() != null ? response.getMessage() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse createTicket(Pega3CreateTicketCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();
        String productoVender = resolveProductoVender(provider);
        String deviceId = requiredValue(provider.getAuth().getLogin().getUsername(), "auth.login.username");
        String channel = resolveChannel(provider);

        Pega3CreateTicketRequest request = Pega3CreateTicketRequest.builder()
                .deviceId(deviceId)
                .token(token)
                .productoVender(productoVender)
                .customerSessionId(command.getUuid())
                .cost(command.getAmount())
                .entryType(command.getEntryType())
                .channel(channel)
                .mainGame(buildMainGame(command))
                .build();

        Pega3CreateTicketResponse response = invokePega3(
                operationPath, provider, request, Pega3CreateTicketResponse.class,
                "createTicket", "creacion ticket Pega3", command.getUuid(), WS_KEY_CREATE_TICKET);

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = hasError(response.getMessage());
        payload.put("message", response.getMessage());
        payload.put("ticket_number", response.getTicketNumber());
        payload.put("game_ticket_number", response.getGameTicketNumber());
        payload.put("cost", response.getCost());
        payload.put("draw_date", response.getDrawDate());
        payload.put("status", response.getStatus());
        payload.put("authorization", response.getGameTicketNumber());
        payload.put("ticket_qr", response.getCodigoQR());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(isError ? "ERROR" : "0")
                .externalMessage(response.getMessage() != null ? response.getMessage() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse payTicket(Pega3PayTicketCommand command, String operationPath, String wsKey) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();
        String productoVender = resolveProductoVender(provider);
        String deviceId = requiredValue(provider.getAuth().getLogin().getUsername(), "auth.login.username");

        Pega3PayTicketRequest request = Pega3PayTicketRequest.builder()
                .deviceId(deviceId)
                .token(token)
                .customerSessionId(command.getUuid())
                .productoVender(productoVender)
                .ticketNumber(requiredValue(command.getTicketNumber(), "ticketNumber"))
                .amount(command.getAmount())
                .build();

        Pega3PayTicketResponse response = invokePega3(
                operationPath, provider, request, Pega3PayTicketResponse.class,
                "payTicket", "pago ticket Pega3", command.getUuid(), wsKey);

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = hasError(response.getMessage());
        payload.put("message", response.getMessage());
        payload.put("authorization", response.getGameTicketNumber());
        payload.put("total_claimed_amount", response.getTotalClaimedAmount());
        payload.put("claimed_on", response.getClaimedOn());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(isError ? "ERROR" : "0")
                .externalMessage(response.getMessage() != null ? response.getMessage() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse verifyTicket(Pega3VerifyTicketCommand command, String operationPath, String wsKey) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();
        String productoVender = resolveProductoVender(provider);

        Pega3VerifyTicketRequest request = Pega3VerifyTicketRequest.builder()
                .token(token)
                .productoVender(productoVender)
                .ticketNumber(requiredValue(command.getTicketNumber(), "ticketNumber"))
                .customerSessionId(command.getUuid())
                .build();

        Pega3VerifyTicketResponse response = invokePega3(
                operationPath, provider, request, Pega3VerifyTicketResponse.class,
                "verifyTicket", "consulta ticket Pega3", command.getUuid(), wsKey);

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = hasError(response.getMessage());
        payload.put("message", response.getMessage());
        payload.put("ticket_number", response.getTicketNumber());
        payload.put("authorization", response.getGameTicketNumber());
        payload.put("ticket_status", response.getStatus());
        payload.put("is_winner", response.getIsWinner());
        payload.put("prize_amount", response.getPrizeAmount());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(isError ? "ERROR" : "0")
                .externalMessage(response.getMessage() != null ? response.getMessage() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse generarComprobante(Pega3ComprobanteQueryCommand command, String operationPath) {
        String ventaId = requiredValue(command.getVentaId(), "ventaId");
        String idUsuario = requiredValue(command.getIdUsuario(), "idUsuario");
        String transaccion = requiredValue(command.getTransaccion(), "transaccion");
        StringBuilder url = new StringBuilder(operationPath)
                .append("?ventaId=").append(encodeParam(ventaId))
                .append("&idUsuario=").append(encodeParam(idUsuario))
                .append("&transaccion=").append(encodeParam(transaccion));
        if (command.getPuntoDeVenta() != null && !command.getPuntoDeVenta().isBlank()) {
            url.append("&puntoDeVenta=").append(encodeParam(command.getPuntoDeVenta()));
        }
        String fullUrl = url.toString();

        log.info("Pega3 generarComprobante GET url={}", fullUrl);
        long startMs = System.currentTimeMillis();

        Pega3ComprobanteResponse response;
        try {
            response = providerCircuitBreaker.execute(PROVIDER_KEY, () ->
                    omnistackWebClient.get()
                            .uri(fullUrl)
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> {
                                        int httpStatus = clientResponse.statusCode().value();
                                        traceErrorToConsole("Pega3 generarComprobante error HTTP " + httpStatus, fullUrl, body);
                                        String errMsg = "Error HTTP " + httpStatus + " al invocar GenerarComprobantePega"
                                                + (body.isBlank() ? " (respuesta sin cuerpo)" : ": " + body);
                                        wsExtLogService.log(ProviderCallLog.builder()
                                                .uuid(command.getUuid())
                                                .providerKey(PROVIDER_KEY)
                                                .wsKey(WS_KEY_VERIFY_COMPROBANTE)
                                                .url(fullUrl)
                                                .requestJson(null)
                                                .responseJson(body)
                                                .durationMs(System.currentTimeMillis() - startMs)
                                                .httpStatus(httpStatus)
                                                .isError(true)
                                                .errorMessage(errMsg)
                                                .build());
                                        return Mono.error(new IntegrationException(errMsg));
                                    }))
                            .bodyToMono(Pega3ComprobanteResponse.class)
                            .block());
        } catch (WebClientRequestException exception) {
            String errMsg = "Error de conexion al invocar GenerarComprobantePega: " + rootCauseMessage(exception);
            wsExtLogService.log(ProviderCallLog.builder()
                    .uuid(command.getUuid())
                    .providerKey(PROVIDER_KEY)
                    .wsKey(WS_KEY_VERIFY_COMPROBANTE)
                    .url(fullUrl)
                    .requestJson(null)
                    .responseJson(null)
                    .durationMs(System.currentTimeMillis() - startMs)
                    .isError(true)
                    .errorMessage(errMsg)
                    .build());
            throw new IntegrationException(errMsg, exception);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = response == null || response.getBase64() == null || response.getBase64().isBlank();
        wsExtLogService.log(ProviderCallLog.builder()
                .uuid(command.getUuid())
                .providerKey(PROVIDER_KEY)
                .wsKey(WS_KEY_VERIFY_COMPROBANTE)
                .url(fullUrl)
                .requestJson(null)
                .responseJson(isError ? null : "[pdf " + response.getFileName() + "]")
                .durationMs(System.currentTimeMillis() - startMs)
                .isError(isError)
                .errorMessage(isError ? "GenerarComprobantePega no retorno contenido" : null)
                .build());
        if (!isError) {
            payload.put("comprobante_b64", response.getBase64());
            payload.put("file_name", response.getFileName());
            payload.put("content_type", response.getContentType());
        }

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(isError ? "ERROR" : "0")
                .externalMessage(isError ? "GenerarComprobantePega no retorno contenido" : "")
                .payload(payload)
                .build();
    }

    private String encodeParam(String value) {
        return value != null ? java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8) : "";
    }

    @Override
    public ExternalTransactionResponse cancelTicket(Pega3CancelTicketCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();
        String productoVender = resolveProductoVender(provider);
        String deviceId = requiredValue(provider.getAuth().getLogin().getUsername(), "auth.login.username");

        Pega3CancelTicketRequest request = Pega3CancelTicketRequest.builder()
                .deviceId(deviceId)
                .token(token)
                .productoVender(productoVender)
                .ticketNumber(requiredValue(command.getTicketNumber(), "ticketNumber"))
                .customerSessionId(command.getUuid())
                .build();

        Pega3CancelTicketResponse response = invokePega3(
                operationPath, provider, request, Pega3CancelTicketResponse.class,
                "cancelTicket", "cancelacion ticket Pega3", command.getUuid(), WS_KEY_REVERSE);

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = hasError(response.getMessage());
        payload.put("message", response.getMessage());
        payload.put("authorization", response.getGameTicketNumber());
        payload.put("refunded_amount", response.getRefundedAmount());
        payload.put("canceled_on", response.getCanceledOn());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(isError ? "ERROR" : "0")
                .externalMessage(response.getMessage() != null ? response.getMessage() : "")
                .payload(payload)
                .build();
    }

    private <T> T invokePega3(
            String operationPath,
            AppProperties.ProviderProperties provider,
            Object request,
            Class<T> responseType,
            String logOperation,
            String errorOperation,
            String uuid,
            String wsKey) {
        String url = operationPath;
        long startMs = System.currentTimeMillis();
        String requestJson = JsonUtil.toJsonSilently(request);
        traceToConsole("Pega3 " + logOperation + " request", url, requestJson);

        // Se captura el body crudo del proveedor ANTES de parsearlo a la clase T — igual patron
        // que TradicionalWebClientAdapter.invokePost(). Antes se mapeaba a T dentro de la propia
        // cadena reactiva y se logueaba la re-serializacion de T (no el JSON real del proveedor),
        // lo que ocultaba silenciosamente cualquier campo que el proveedor mandara y no estuviera
        // ya modelado en el DTO — imposible depurar campos no documentados de esta forma.
        String rawBody;
        try {
            rawBody = providerCircuitBreaker.execute(PROVIDER_KEY, () ->
                    omnistackWebClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> {
                                        int httpStatus = clientResponse.statusCode().value();
                                        traceErrorToConsole("Pega3 " + logOperation + " error HTTP " + httpStatus, url, body);
                                        String errMsg = "Error HTTP " + httpStatus + " al invocar " + errorOperation
                                                + (body.isBlank() ? " (respuesta sin cuerpo)" : ": " + body);
                                        wsExtLogService.log(ProviderCallLog.builder()
                                                .uuid(uuid)
                                                .providerKey(PROVIDER_KEY)
                                                .wsKey(wsKey)
                                                .url(url)
                                                .requestJson(requestJson)
                                                .responseJson(body)
                                                .durationMs(System.currentTimeMillis() - startMs)
                                                .httpStatus(httpStatus)
                                                .isError(true)
                                                .errorMessage(errMsg)
                                                .build());
                                        return Mono.error(new IntegrationException(errMsg));
                                    }))
                            .bodyToMono(String.class)
                            .block());
        } catch (WebClientRequestException exception) {
            traceErrorToConsole("Pega3 " + logOperation + " transport error", url, rootCauseMessage(exception));
            String errMsg = buildTransportErrorMessage(url, exception, errorOperation);
            wsExtLogService.log(ProviderCallLog.builder()
                    .uuid(uuid)
                    .providerKey(PROVIDER_KEY)
                    .wsKey(wsKey)
                    .url(url)
                    .requestJson(requestJson)
                    .responseJson(null)
                    .durationMs(System.currentTimeMillis() - startMs)
                    .isError(true)
                    .errorMessage(errMsg)
                    .build());
            throw new IntegrationException(errMsg, exception);
        }

        traceToConsole("Pega3 " + logOperation + " response", url, rawBody);
        wsExtLogService.log(ProviderCallLog.builder()
                .uuid(uuid)
                .providerKey(PROVIDER_KEY)
                .wsKey(wsKey)
                .url(url)
                .requestJson(requestJson)
                .responseJson(rawBody)
                .durationMs(System.currentTimeMillis() - startMs)
                .isError(false)
                .build());

        return parseResponse(rawBody, responseType, errorOperation);
    }

    private Pega3CreateTicketRequest.MainGame buildMainGame(Pega3CreateTicketCommand command) {
        List<Pega3CreateTicketRequest.Panel> externalPanels = new ArrayList<>();
        String entryType = command.getEntryType();
        boolean quickPick = entryType != null && entryType.toLowerCase().contains("quickpick");
        int typeOfEntryInt = quickPick ? TYPE_OF_ENTRY_QUICK_PICK : TYPE_OF_ENTRY_MANUAL;
        String betType = quickPick ? "QuickPick" : "Manual";

        for (Pega3Panel panel : command.getPanels()) {
            Pega3CreateTicketRequest.Entry entry = Pega3CreateTicketRequest.Entry.builder()
                    .type(ENTRY_TYPE_REGULAR)
                    .quickPick(quickPick)
                    .playTypes(panel.getPlayTypes())
                    .value(panel.getNumbers())
                    .build();

            externalPanels.add(Pega3CreateTicketRequest.Panel.builder()
                    .betType(betType)
                    .typeOfEntry(typeOfEntryInt)
                    .betAmount(panel.getBetAmount())
                    .entries(List.of(entry))
                    .build());
        }

        return Pega3CreateTicketRequest.MainGame.builder()
                .code(GAME_CODE)
                .advanceDraw(ADVANCE_DRAW)
                .noOfDraws(NO_OF_DRAWS)
                .panels(externalPanels)
                .addOns(List.of())
                .build();
    }

    private <T> T parseResponse(String body, Class<T> type, String operation) {
        if (body == null || body.isBlank()) {
            throw new IntegrationException("Pega3 no retorno contenido para " + operation);
        }
        try {
            T result = objectMapper.readValue(body, type);
            if (result == null) {
                throw new IntegrationException("Pega3 " + operation + " retorno null — sorteo posiblemente invalido");
            }
            return result;
        } catch (JsonProcessingException exception) {
            throw new IntegrationException("Pega3 " + operation + " retorno un body no parseable");
        }
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(PROVIDER_KEY);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor Pega3");
        }
        return provider;
    }

    private String resolveToken() {
        return providerTokenResolverUseCase.getToken(PROVIDER_KEY);
    }

    private String resolveProductoVender(AppProperties.ProviderProperties provider) {
        if (provider.getAuth() == null || provider.getAuth().getLogin() == null
                || provider.getAuth().getLogin().getProductToSell() == null
                || provider.getAuth().getLogin().getProductToSell().isBlank()) {
            throw new IntegrationException("Pega3 requiere auth.login.productToSell configurado");
        }
        return provider.getAuth().getLogin().getProductToSell();
    }

    private String resolveChannel(AppProperties.ProviderProperties provider) {
        return provider.getCanal() != null ? provider.getCanal() : GAME_CODE;
    }

    private boolean hasError(String message) {
        return message != null && !message.isBlank();
    }


    private String requiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IntegrationException("Pega3 requiere el campo " + fieldName);
        }
        return value;
    }

    private String buildTransportErrorMessage(String url, WebClientRequestException exception, String operation) {
        if (hasCause(exception, "ReadTimeoutException")) {
            return "Timeout al invocar " + operation + " de Pega3: " + url;
        }
        if (hasCause(exception, "SSLHandshakeException") || hasCause(exception, "SunCertPathBuilderException")) {
            return "Error SSL al invocar " + operation + " de Pega3. Revise el certificado/truststore para " + url;
        }
        return "Error de conexion al invocar " + operation + " de Pega3: " + rootCauseMessage(exception);
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

    private void traceToConsole(String label, String url, String body) {
        log.info("{} url={} body={}", label, url, body);
    }

    private void traceErrorToConsole(String label, String url, String body) {
        log.error("{} url={} body={}", label, url, body);
    }
}

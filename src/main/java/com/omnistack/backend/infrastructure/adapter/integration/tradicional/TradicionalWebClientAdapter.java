package com.omnistack.backend.infrastructure.adapter.integration.tradicional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.TradicionalAnularVentaPort;
import com.omnistack.backend.application.port.out.TradicionalConsultarTicketPort;
import com.omnistack.backend.application.port.out.TradicionalFigurasQueryPort;
import com.omnistack.backend.application.port.out.TradicionalJuegoQueryPort;
import com.omnistack.backend.application.port.out.TradicionalNumerosQueryPort;
import com.omnistack.backend.application.port.out.TradicionalPagoPremioPort;
import com.omnistack.backend.application.port.out.TradicionalSorteosQueryPort;
import com.omnistack.backend.application.port.out.TradicionalVentaBoletosPort;
import com.omnistack.backend.application.port.out.TradicionalVerifyPort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.WsExtLogService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ProviderCallLog;
import com.omnistack.backend.domain.model.TradicionalAnularVentaCommand;
import com.omnistack.backend.domain.model.TradicionalConsultarTicketCommand;
import com.omnistack.backend.domain.model.TradicionalFigurasQueryCommand;
import com.omnistack.backend.domain.model.TradicionalJuegoQueryCommand;
import com.omnistack.backend.domain.model.TradicionalNumerosQueryCommand;
import com.omnistack.backend.domain.model.TradicionalPagoPremioCommand;
import com.omnistack.backend.domain.model.TradicionalSorteosQueryCommand;
import com.omnistack.backend.domain.model.TradicionalVentaBoletosCommand;
import com.omnistack.backend.domain.model.TradicionalVerifyCommand;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalAnularVentaRequest;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalAnularVentaResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalComprobanteResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalConsultarTicketRequest;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalConsultarTicketResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalFigurasQueryRequest;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalFigurasQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalJuegoQueryRequest;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalJuegoQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalNumerosQueryRequest;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalNumerosQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalPagoPremioRequest;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalPagoPremioResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalSorteosQueryRequest;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalSorteosQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalVentaBoletosRequest;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalVentaBoletosResponse;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Adapter HTTP para todas las operaciones del proveedor LN Tradicionales.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradicionalWebClientAdapter implements
        TradicionalJuegoQueryPort,
        TradicionalSorteosQueryPort,
        TradicionalFigurasQueryPort,
        TradicionalNumerosQueryPort,
        TradicionalVentaBoletosPort,
        TradicionalAnularVentaPort,
        TradicionalVerifyPort,
        TradicionalConsultarTicketPort,
        TradicionalPagoPremioPort {

    private static final String PROVIDER_KEY = "tradicional";
    private static final String WS_KEY_PRECHECK = "PRECHECK.CASHIN";
    private static final String WS_KEY_PRECHECK_SORTEOS = "PRECHECK_SORTEOS.CASHIN";
    private static final String WS_KEY_PRECHECK_FIGURAS = "PRECHECK_FIGURAS.CASHIN";
    private static final String WS_KEY_PRECHECK_NUMEROS = "PRECHECK_NUMEROS.CASHIN";
    private static final String WS_KEY_EXECUTE = "EXECUTE.CASHIN";
    private static final String WS_KEY_REVERSE = "REVERSE.CASHIN";
    private static final String WS_KEY_VERIFY = "VERIFY.CASHIN";
    private static final String WS_KEY_PRECHECK_CASHOUT = "PRECHECK.CASHOUT";
    private static final String WS_KEY_EXECUTE_CASHOUT = "EXECUTE.CASHOUT";
    private static final String PRODUCTO_VENDER_TRADICIONALES = "Tradicionales";

    private final WebClient omnistackWebClient;
    private final ProviderConfigService providerConfigService;
    private final ObjectMapper objectMapper;
    private final ProviderTokenResolverUseCase providerTokenResolverUseCase;
    private final WsExtLogService wsExtLogService;

    @Override
    public ExternalTransactionResponse queryJuegos(TradicionalJuegoQueryCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();

        TradicionalJuegoQueryRequest request = TradicionalJuegoQueryRequest.builder()
                .userName(resolveUserName(provider))
                .token(token)
                .medioId(String.valueOf(provider.getMedioId()))
                .build();

        TradicionalJuegoQueryResponse response = invokePost(
                operationPath, provider, request, TradicionalJuegoQueryResponse.class,
                "queryJuegos", "consulta juegos Tradicionales", command.getUuid(), WS_KEY_PRECHECK);

        if (isInvalidTokenMessage(response.getMsgError())) {
            String freshToken = providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            TradicionalJuegoQueryRequest retryRequest = request.toBuilder().token(freshToken).build();
            response = invokePost(
                    operationPath, provider, retryRequest, TradicionalJuegoQueryResponse.class,
                    "queryJuegos retry", "consulta juegos Tradicionales", command.getUuid(), WS_KEY_PRECHECK);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = !isSuccess(response.getCodError())
                || response.getListaDetalle() == null || response.getListaDetalle().isEmpty();
        payload.put("juegos", response.getListaDetalle() != null ? response.getListaDetalle() : Collections.emptyList());
        payload.put("codError", response.getCodError());
        payload.put("msgError", response.getMsgError());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(String.valueOf(response.getCodError()))
                .externalMessage(response.getMsgError() != null ? response.getMsgError() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse querySorteos(TradicionalSorteosQueryCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();

        TradicionalSorteosQueryRequest request = TradicionalSorteosQueryRequest.builder()
                .userName(resolveUserName(provider))
                .token(token)
                .medioId(String.valueOf(provider.getMedioId()))
                .juegoId(command.getJuegoId())
                .build();

        TradicionalSorteosQueryResponse response = invokePost(
                operationPath, provider, request, TradicionalSorteosQueryResponse.class,
                "querySorteos", "consulta sorteos Tradicionales", command.getUuid(), WS_KEY_PRECHECK_SORTEOS);

        if (isInvalidTokenMessage(response.getMsgError())) {
            String freshToken = providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            TradicionalSorteosQueryRequest retryRequest = request.toBuilder().token(freshToken).build();
            response = invokePost(
                    operationPath, provider, retryRequest, TradicionalSorteosQueryResponse.class,
                    "querySorteos retry", "consulta sorteos Tradicionales", command.getUuid(), WS_KEY_PRECHECK_SORTEOS);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = !isSuccess(response.getCodError());
        payload.put("listaSorteos", response.getListaSorteos());
        payload.put("codError", response.getCodError());
        payload.put("msgError", response.getMsgError());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(String.valueOf(response.getCodError()))
                .externalMessage(response.getMsgError() != null ? response.getMsgError() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse queryFiguras(TradicionalFigurasQueryCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();

        TradicionalFigurasQueryRequest request = TradicionalFigurasQueryRequest.builder()
                .userName(resolveUserName(provider))
                .token(token)
                .medioId(String.valueOf(provider.getMedioId()))
                .juegoId(command.getJuegoId())
                .build();

        TradicionalFigurasQueryResponse response = invokePost(
                operationPath, provider, request, TradicionalFigurasQueryResponse.class,
                "queryFiguras", "consulta figuras Tradicionales", command.getUuid(), WS_KEY_PRECHECK_FIGURAS);

        if (isInvalidTokenMessage(response.getMsgError())) {
            String freshToken = providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            TradicionalFigurasQueryRequest retryRequest = request.toBuilder().token(freshToken).build();
            response = invokePost(
                    operationPath, provider, retryRequest, TradicionalFigurasQueryResponse.class,
                    "queryFiguras retry", "consulta figuras Tradicionales", command.getUuid(), WS_KEY_PRECHECK_FIGURAS);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = !isSuccess(response.getCodError());
        payload.put("listaFiguras", response.getListaFiguras());
        payload.put("codError", response.getCodError());
        payload.put("msgError", response.getMsgError());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(String.valueOf(response.getCodError()))
                .externalMessage(response.getMsgError() != null ? response.getMsgError() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse queryNumeros(TradicionalNumerosQueryCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();

        TradicionalNumerosQueryRequest request = TradicionalNumerosQueryRequest.builder()
                .userName(resolveUserName(provider))
                .token(token)
                .medioId(String.valueOf(provider.getMedioId()))
                .juegoId(command.getJuegoId())
                .sorteoId(command.getSorteoId())
                .combinacion(command.getCombinacion())
                .combinacionFigura(command.getCombinacionFigura())
                .sugerir(command.getSugerir())
                .cantidad(command.getCantidad())
                .registros(command.getRegistros())
                .build();

        TradicionalNumerosQueryResponse response = invokePost(
                operationPath, provider, request, TradicionalNumerosQueryResponse.class,
                "queryNumeros", "consulta numeros Tradicionales", command.getUuid(), WS_KEY_PRECHECK_NUMEROS);

        if (isInvalidTokenMessage(response.getMsgError())) {
            String freshToken = providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            TradicionalNumerosQueryRequest retryRequest = request.toBuilder().token(freshToken).build();
            response = invokePost(
                    operationPath, provider, retryRequest, TradicionalNumerosQueryResponse.class,
                    "queryNumeros retry", "consulta numeros Tradicionales", command.getUuid(), WS_KEY_PRECHECK_NUMEROS);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = !isSuccess(response.getCodError());
        payload.put("listaNumeros", response.getListaNumeros());
        payload.put("totalResults", response.getTotalResults());
        payload.put("numeroReserva", response.getNumeroReserva());
        payload.put("codError", response.getCodError());
        payload.put("msgError", response.getMsgError());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(String.valueOf(response.getCodError()))
                .externalMessage(response.getMsgError() != null ? response.getMsgError() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse ventaBoletos(TradicionalVentaBoletosCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();

        TradicionalVentaBoletosRequest request = TradicionalVentaBoletosRequest.builder()
                .userName(resolveUserName(provider))
                .token(token)
                .medioId(String.valueOf(provider.getMedioId()))
                .reservaId(command.getReservaId())
                .cliente(command.getCliente())
                .totalVenta(command.getTotalVenta())
                .numeroIdentificacion(command.getNumeroIdentificacion())
                .nombreComprador(command.getNombreComprador())
                .numeroCelularComprador(command.getNumeroCelularComprador())
                .correoElectronicoComprador(command.getCorreoElectronicoComprador())
                .listaOrdenCompra(List.of(
                        TradicionalVentaBoletosRequest.OrdenCompra.builder()
                                .ordenCompra(command.getOrdenCompra())
                                .listaFormaCobro(List.of(
                                        TradicionalVentaBoletosRequest.FormaCobro.builder()
                                                .formaCobro(command.getFormaCobro())
                                                .total(command.getTotalVenta())
                                                .build()))
                                .build()))
                .listaJuegos(buildListaJuegos(command.getBoletos()))
                .build();

        TradicionalVentaBoletosResponse response = invokePost(
                operationPath, provider, request, TradicionalVentaBoletosResponse.class,
                "ventaBoletos", "venta boletos Tradicionales", command.getUuid(), WS_KEY_EXECUTE);

        if (isInvalidTokenMessage(response.getMsgError())) {
            String freshToken = providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            TradicionalVentaBoletosRequest retryRequest = request.toBuilder().token(freshToken).build();
            response = invokePost(
                    operationPath, provider, retryRequest, TradicionalVentaBoletosResponse.class,
                    "ventaBoletos retry", "venta boletos Tradicionales", command.getUuid(), WS_KEY_EXECUTE);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = !response.isSuccess();
        payload.put("codError", response.getCodError());
        payload.put("msgError", response.getMsgError());
        payload.put("authorization", response.getVentaId());
        payload.put("ventaId", response.getVentaId());
        payload.put("fechaVenta", response.getFechaVenta());
        payload.put("transaccion", response.getTransaccion());

        TradicionalVentaBoletosResponse.TicketDetalle primerTicket = firstTicketDetalle(response);
        if (primerTicket != null) {
            payload.put("boletoClave", primerTicket.getClave());
            payload.put("boletoQr", primerTicket.getCodigoQR());
            if (primerTicket.getListaNumeroFracciones() != null && !primerTicket.getListaNumeroFracciones().isEmpty()) {
                payload.put("fraccionesVendidas", primerTicket.getListaNumeroFracciones().get(0).getNumeroFraccion());
            }
        }

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(String.valueOf(response.getCodError()))
                .externalMessage(response.getMsgError() != null ? response.getMsgError() : "")
                .payload(payload)
                .build();
    }

    private List<TradicionalVentaBoletosRequest.JuegoEntry> buildListaJuegos(
            List<TradicionalVentaBoletosCommand.BoletoEntry> boletos) {
        Map<String, List<TradicionalVentaBoletosRequest.SorteoEntry>> sorteosPorJuego = new LinkedHashMap<>();
        for (TradicionalVentaBoletosCommand.BoletoEntry boleto : boletos) {
            sorteosPorJuego
                    .computeIfAbsent(boleto.getJuegoId(), key -> new java.util.ArrayList<>())
                    .add(TradicionalVentaBoletosRequest.SorteoEntry.builder()
                            .sorteoId(boleto.getSorteoId())
                            .numero(boleto.getNumero())
                            .cantidadBoletos(boleto.getCantidadBoletos())
                            .build());
        }
        return sorteosPorJuego.entrySet().stream()
                .map(entry -> TradicionalVentaBoletosRequest.JuegoEntry.builder()
                        .juegoId(entry.getKey())
                        .listaSorteos(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private TradicionalVentaBoletosResponse.TicketDetalle firstTicketDetalle(TradicionalVentaBoletosResponse response) {
        if (response.getListaSUE() == null || response.getListaSUE().isEmpty()) {
            return null;
        }
        TradicionalVentaBoletosResponse.Sue sue = response.getListaSUE().get(0);
        if (sue.getListaSorteos() == null || sue.getListaSorteos().isEmpty()) {
            return null;
        }
        TradicionalVentaBoletosResponse.SorteoDetalle sorteoDetalle = sue.getListaSorteos().get(0);
        if (sorteoDetalle.getListaR() == null || sorteoDetalle.getListaR().isEmpty()) {
            return null;
        }
        return sorteoDetalle.getListaR().get(0);
    }

    @Override
    public ExternalTransactionResponse anularVenta(TradicionalAnularVentaCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();

        TradicionalAnularVentaRequest request = TradicionalAnularVentaRequest.builder()
                .userName(resolveUserName(provider))
                .token(token)
                .medioId(String.valueOf(provider.getMedioId()))
                .clienteId(command.getClienteId())
                .ordenCompra(command.getOrdenCompra())
                .motivo(command.getMotivo())
                .build();

        TradicionalAnularVentaResponse response = invokePost(
                operationPath, provider, request, TradicionalAnularVentaResponse.class,
                "anularVenta", "anulacion venta Tradicionales", command.getUuid(), WS_KEY_REVERSE);

        if (isInvalidTokenMessage(response.getMsgError())) {
            String freshToken = providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            TradicionalAnularVentaRequest retryRequest = request.toBuilder().token(freshToken).build();
            response = invokePost(
                    operationPath, provider, retryRequest, TradicionalAnularVentaResponse.class,
                    "anularVenta retry", "anulacion venta Tradicionales", command.getUuid(), WS_KEY_REVERSE);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = !response.isSuccess();
        payload.put("codError", response.getCodError());
        payload.put("msgError", response.getMsgError());
        payload.put("transaccion", response.getTransaccion());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(String.valueOf(response.getCodError()))
                .externalMessage(response.getMsgError() != null ? response.getMsgError() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse consultarTicket(TradicionalConsultarTicketCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();

        TradicionalConsultarTicketRequest request = TradicionalConsultarTicketRequest.builder()
                .token(token)
                .productoVender(PRODUCTO_VENDER_TRADICIONALES)
                .userId(command.getUserId())
                .clienteId(command.getClienteId())
                .mor(command.getMor())
                .tipoDocumento(command.getTipoDocumento())
                .numeroDocumento(command.getNumeroDocumento())
                .nombreGanador(command.getNombreGanador())
                .cl(command.getCl())
                .cb(command.getCb())
                .co(command.getCo())
                .premioEspecie(command.getPremioEspecie())
                .build();

        TradicionalConsultarTicketResponse response = invokePost(
                operationPath, provider, request, TradicionalConsultarTicketResponse.class,
                "consultarTicket", "consulta ticket premio Tradicionales", command.getUuid(), WS_KEY_PRECHECK_CASHOUT);

        if (isInvalidTokenMessage(response.getMsgError())) {
            String freshToken = providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            TradicionalConsultarTicketRequest retryRequest = request.toBuilder().token(freshToken).build();
            response = invokePost(
                    operationPath, provider, retryRequest, TradicionalConsultarTicketResponse.class,
                    "consultarTicket retry", "consulta ticket premio Tradicionales", command.getUuid(), WS_KEY_PRECHECK_CASHOUT);
        }

        return buildConsultarTicketResponse(response);
    }

    private ExternalTransactionResponse buildConsultarTicketResponse(TradicionalConsultarTicketResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("codError", response.getCodError());
        payload.put("msgError", response.getMsgError());

        TradicionalConsultarTicketResponse.Bpr boleto = firstBoleto(response);
        boolean tieneError = !response.isSuccess()
                || boleto == null
                || !("0".equals(boleto.getCod()) || boleto.getCod() == null);

        String externalMessage = response.getMsgError();
        if (boleto != null && boleto.getMen() != null && !boleto.getMen().isBlank()) {
            externalMessage = boleto.getMen();
        }

        if (!tieneError) {
            payload.put("mpi", response.getResultado() != null && response.getResultado().getPpr() != null
                    ? response.getResultado().getPpr().getMpi() : null);
            payload.put("is_winner", true);
            payload.put("ticket_status", boleto.getMen());
            payload.put("prize_amount", boleto.getMon());
            payload.put("authorization", boleto.getCla());
        } else {
            payload.put("is_winner", false);
            payload.put("ticket_status", boleto != null ? boleto.getMen() : externalMessage);
        }

        return ExternalTransactionResponse.builder()
                .approved(!tieneError)
                .externalCode(String.valueOf(response.getCodError()))
                .externalMessage(externalMessage != null ? externalMessage : "")
                .payload(payload)
                .build();
    }

    private TradicionalConsultarTicketResponse.Bpr firstBoleto(TradicionalConsultarTicketResponse response) {
        if (response.getResultado() == null || response.getResultado().getPpr() == null
                || response.getResultado().getPpr().getBpr() == null
                || response.getResultado().getPpr().getBpr().isEmpty()) {
            return null;
        }
        return response.getResultado().getPpr().getBpr().get(0);
    }

    @Override
    public ExternalTransactionResponse pagoPremio(TradicionalPagoPremioCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String token = resolveToken();

        TradicionalPagoPremioRequest request = TradicionalPagoPremioRequest.builder()
                .token(token)
                .productoVender(PRODUCTO_VENDER_TRADICIONALES)
                .userId(command.getUserId())
                .clienteId(command.getClienteId())
                .mor(command.getMor())
                .tipoDocumento(command.getTipoDocumento())
                .numeroDocumento(command.getNumeroDocumento())
                .nombreGanador(command.getNombreGanador())
                .cl(command.getCl())
                .cb(command.getCb())
                .co(command.getCo())
                .premioEspecie(command.getPremioEspecie())
                .mpi(command.getMpi())
                .build();

        TradicionalPagoPremioResponse response = invokePost(
                operationPath, provider, request, TradicionalPagoPremioResponse.class,
                "pagoPremio", "pago premio Tradicionales", command.getUuid(), WS_KEY_EXECUTE_CASHOUT);

        if (isInvalidTokenMessage(response.getMsgError())) {
            String freshToken = providerTokenResolverUseCase.refreshToken(PROVIDER_KEY);
            TradicionalPagoPremioRequest retryRequest = request.toBuilder().token(freshToken).build();
            response = invokePost(
                    operationPath, provider, retryRequest, TradicionalPagoPremioResponse.class,
                    "pagoPremio retry", "pago premio Tradicionales", command.getUuid(), WS_KEY_EXECUTE_CASHOUT);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        boolean isError = !response.isSuccess();
        payload.put("codError", response.getCodError());
        payload.put("msgError", response.getMsgError());
        if (response.getResultado() != null && response.getResultado().getPpr() != null) {
            payload.put("mpi", response.getResultado().getPpr().getMpi());
            payload.put("pci", response.getResultado().getPpr().getPci());
            payload.put("ref", response.getResultado().getPpr().getRef());
            payload.put("authorization", response.getResultado().getPpr().getPci());
        }

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(String.valueOf(response.getCodError()))
                .externalMessage(response.getMsgError() != null ? response.getMsgError() : "")
                .payload(payload)
                .build();
    }

    @Override
    public ExternalTransactionResponse generateComprobante(TradicionalVerifyCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String url = operationPath;
        String fullUrl = url + "?ventaId=" + command.getVentaId()
                + "&idUsuario=" + encodeParam(command.getIdUsuario())
                + "&puntoDeVenta=" + encodeParam(command.getPuntoDeVenta());

        log.info("Tradicionales generateComprobante GET url={}", fullUrl);
        long startMs = System.currentTimeMillis();

        TradicionalComprobanteResponse response;
        try {
            response = omnistackWebClient.get()
                    .uri(fullUrl)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                log.error("Tradicionales generateComprobante error url={} body={}", fullUrl, body);
                                String errMsg = "Error HTTP al invocar GenerarComprobanteVenta: " + body;
                                wsExtLogService.log(ProviderCallLog.builder()
                                        .uuid(command.getUuid())
                                        .providerKey(PROVIDER_KEY)
                                        .wsKey(WS_KEY_VERIFY)
                                        .url(fullUrl)
                                        .requestJson(null)
                                        .responseJson(body)
                                        .durationMs(System.currentTimeMillis() - startMs)
                                        .isError(true)
                                        .errorMessage(errMsg)
                                        .build());
                                return Mono.error(new IntegrationException(errMsg));
                            }))
                    .bodyToMono(TradicionalComprobanteResponse.class)
                    .block();
        } catch (WebClientRequestException exception) {
            String errMsg = "Error de conexion al invocar GenerarComprobanteVenta: " + rootCauseMessage(exception);
            wsExtLogService.log(ProviderCallLog.builder()
                    .uuid(command.getUuid())
                    .providerKey(PROVIDER_KEY)
                    .wsKey(WS_KEY_VERIFY)
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
                .wsKey(WS_KEY_VERIFY)
                .url(fullUrl)
                .requestJson(null)
                .responseJson(isError ? null : "[pdf " + response.getFileName() + "]")
                .durationMs(System.currentTimeMillis() - startMs)
                .isError(isError)
                .errorMessage(isError ? "GenerarComprobanteVenta no retorno contenido" : null)
                .build());
        if (!isError) {
            payload.put("comprobante_b64", response.getBase64());
            payload.put("file_name", response.getFileName());
        }
        payload.put("ventaId", command.getVentaId());

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(isError ? "ERROR" : "0")
                .externalMessage(isError ? "GenerarComprobanteVenta no retorno contenido" : "")
                .payload(payload)
                .build();
    }

    private String invokePost(
            String operationPath,
            AppProperties.ProviderProperties provider,
            Object request,
            String logOperation,
            String errorOperation,
            String uuid,
            String wsKey) {
        String url = operationPath;
        long startMs = System.currentTimeMillis();
        String requestJson = JsonUtil.toJsonSilently(request);
        log.info("Tradicionales {} request url={} body={}", logOperation, url, requestJson);

        String body;
        try {
            body = omnistackWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(b -> {
                                log.error("Tradicionales {} error url={} body={}", logOperation, url, b);
                                String errMsg = "Error HTTP al invocar " + errorOperation + ": " + b;
                                wsExtLogService.log(ProviderCallLog.builder()
                                        .uuid(uuid)
                                        .providerKey(PROVIDER_KEY)
                                        .wsKey(wsKey)
                                        .url(url)
                                        .requestJson(requestJson)
                                        .responseJson(b)
                                        .durationMs(System.currentTimeMillis() - startMs)
                                        .isError(true)
                                        .errorMessage(errMsg)
                                        .build());
                                return Mono.error(new IntegrationException(errMsg));
                            }))
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientRequestException exception) {
            String errMsg = "Error de conexion al invocar " + errorOperation + ": " + rootCauseMessage(exception);
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

        if (body == null || body.isBlank()) {
            throw new IntegrationException("Tradicionales no retorno contenido para " + errorOperation);
        }
        log.info("Tradicionales {} response url={} body={}", logOperation, url, body);
        wsExtLogService.log(ProviderCallLog.builder()
                .uuid(uuid)
                .providerKey(PROVIDER_KEY)
                .wsKey(wsKey)
                .url(url)
                .requestJson(requestJson)
                .responseJson(body)
                .durationMs(System.currentTimeMillis() - startMs)
                .isError(false)
                .build());
        return body;
    }

    private <T> T invokePost(
            String operationPath,
            AppProperties.ProviderProperties provider,
            Object request,
            Class<T> responseType,
            String logOperation,
            String errorOperation,
            String uuid,
            String wsKey) {
        String body = invokePost(operationPath, provider, request, logOperation, errorOperation, uuid, wsKey);
        try {
            return objectMapper.readValue(body, responseType);
        } catch (JsonProcessingException e) {
            throw new IntegrationException("Tradicionales " + errorOperation + " retorno un body no parseable");
        }
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(PROVIDER_KEY);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor Tradicionales");
        }
        return provider;
    }

    private String resolveToken() {
        return providerTokenResolverUseCase.getToken(PROVIDER_KEY);
    }

    private String resolveUserName(AppProperties.ProviderProperties provider) {
        if (provider.getAuth() == null || provider.getAuth().getLogin() == null
                || provider.getAuth().getLogin().getUsername() == null
                || provider.getAuth().getLogin().getUsername().isBlank()) {
            throw new IntegrationException("Tradicionales requiere auth.login.username configurado");
        }
        return provider.getAuth().getLogin().getUsername();
    }

    private boolean isSuccess(Object codError) {
        return codError != null && "0".equals(String.valueOf(codError));
    }

    /**
     * Loteria Nacional mantiene una sola sesion activa por usuario: loguearse para
     * Bet593 o Pega3 con la misma cuenta invalida la sesion de Tradicionales aunque
     * nuestro token cacheado no haya vencido segun el TTL local. Ante ese rechazo,
     * forzamos un refresh y reintentamos una vez (mismo patron que
     * Bet593WithdrawWebClientAdapter.isInvalidTokenResponse).
     */
    private boolean isInvalidTokenMessage(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String message = value.toLowerCase(java.util.Locale.ROOT);
        return message.contains("token")
                && (message.contains("invalid")
                || message.contains("inval")
                || message.contains("expir")
                || message.contains("venc")
                || message.contains("caduc")
                || message.contains("autoriz")
                || message.contains("sesion"));
    }


    private String encodeParam(String value) {
        return value != null ? java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8) : "";
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
}

package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.dto.VerifyRequest;
import com.omnistack.backend.application.dto.VerifyResponse;
import com.omnistack.backend.application.port.out.Pega3ComprobanteQueryPort;
import com.omnistack.backend.application.port.out.Pega3VerifyTicketPort;
import com.omnistack.backend.application.port.out.RegistroTrxPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.VerifyStrategy;
import com.omnistack.backend.application.service.ComprobanteUrlService;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3ComprobanteQueryCommand;
import com.omnistack.backend.domain.model.Pega3VerifyTicketCommand;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Estrategia de VERIFY CASH_IN para Pega3. Llama a ConsultarTicket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoteriaPega3VerifyStrategy extends AbstractProviderStrategy implements VerifyStrategy {

    private static final String PROVIDER_KEY = "pega3";
    private static final String PROVIDER_NAME = "Loteria Pega3";
    private static final String VERIFY_COMPROBANTE_KEY = "VERIFY_COMPROBANTE";

    private final Pega3VerifyTicketPort pega3VerifyTicketPort;
    private final Pega3ComprobanteQueryPort pega3ComprobanteQueryPort;
    private final RegistroTrxPort registroTrxPort;
    private final ComprobanteUrlService comprobanteUrlService;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.VERIFY
                && provider != null
                && serviceDefinition.getMovementType() == MovementType.CASH_IN
                && serviceDefinition.getServiceProviderCode() != null
                && serviceDefinition.getServiceProviderCode().equalsIgnoreCase(provider.getServiceProviderCode())
                && hasConfiguredOperation(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition);
    }

    @Override
    public BaseTransactionResponse process(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            Capability capability) {
        AppProperties.ProviderProperties provider = getProviderProperties(providerConfigService, PROVIDER_KEY, PROVIDER_NAME);

        if (request.getAuthorization() == null || request.getAuthorization().isBlank()) {
            throw new IntegrationException("Pega3 requiere authorization (ticketNumber) para VERIFY");
        }

        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);

        Pega3VerifyTicketCommand command = Pega3VerifyTicketCommand.builder()
                .uuid(request.getUuid())
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode())
                .subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode())
                .rmsItemCode(request.getRmsItemCode())
                .ticketNumber(request.getAuthorization())
                .build();

        String wsKey = toWsKey(capability.name(), serviceDefinition.getMovementType());
        ExternalTransactionResponse externalResponse = pega3VerifyTicketPort.verifyTicket(command, operationUrl, wsKey);
        List<ComprobanteData> comprobantes = fetchComprobanteIfAvailable(request, serviceDefinition, provider, externalResponse);
        return buildResponse(request, externalResponse, comprobantes);
    }

    private record ComprobanteData(String base64, String contentType) {
    }

    /**
     * Genera el comprobante PDF (GenerarComprobantePega) solo si la operacion esta configurada
     * y hay un valor disponible para "transaccion". La respuesta de ConsultarTicket/CrearTicket
     * de Pega3 no incluye ese dato (confirmado contra QA real, 2026-07-15) — se usa el mismo
     * valor de uuid que se envio como customerSessionId al vender el ticket (persistido en
     * IN_OMNI_REGISTRO_TRX por EXECUTE — ahi es donde Pega3 llama CrearTicket, ya no en
     * CREATE_TICKET; recuperado aqui por ticketNumber/authorization) como candidato, ya que
     * "transaccion" se documenta como "codigo de la transaccion asociada
     * a la venta" — es una hipotesis pendiente de confirmar en QA, no un dato oficial del
     * proveedor. El POS puede seguir enviendo "transaccion" explicito en el request, que tiene
     * prioridad sobre este fallback.
     * <p>
     * ventaId: el proveedor rechaza con 404 "No existe una venta con el ID proporcionado" si se
     * usa el ticketNumber completo (el que se genero en CrearTicket, ej.
     * "TO0119001180246130060911133"). ConsultarTicket devuelve un gameTicketNumber TRUNCADO
     * (3 caracteres menos, ej. "TO0119001180246130060911") — confirmado contra QA real
     * (2026-07-19) que es ESE valor truncado el que GenerarComprobantePega espera como ventaId,
     * no el ticketNumber original que el POS reenvia como authorization.
     */
    @SuppressWarnings("unchecked")
    private List<ComprobanteData> fetchComprobanteIfAvailable(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            AppProperties.ProviderProperties provider,
            ExternalTransactionResponse verifyTicketResponse) {
        String ventaId = stringValue(verifyTicketResponse.getPayload(), "authorization");
        if (ventaId == null || ventaId.isBlank()) {
            ventaId = request.getAuthorization();
        }
        String transaccion = request instanceof VerifyRequest verifyRequest ? verifyRequest.getTransaccion() : null;
        boolean transaccionExplicita = transaccion != null && !transaccion.isBlank();
        if (!transaccionExplicita) {
            transaccion = registroTrxPort.findExecuteUuidByAuthorization(request.getAuthorization()).orElse(null);
        }
        if (transaccion == null || transaccion.isBlank()) {
            log.warn("Pega3 VERIFY sin comprobante: no se encontro 'transaccion' (ni explicito en el request, "
                    + "ni via IN_OMNI_REGISTRO_TRX por authorization={})", request.getAuthorization());
            return null;
        }
        log.info("Pega3 VERIFY: transaccion resuelta={} (fuente={})", transaccion, transaccionExplicita ? "request" : "IN_OMNI_REGISTRO_TRX");
        String comprobanteUrl = providerWsService
                .findUrl(PROVIDER_KEY, toWsKey(VERIFY_COMPROBANTE_KEY, serviceDefinition.getMovementType()))
                .orElse(null);
        if (comprobanteUrl == null || comprobanteUrl.isBlank()) {
            log.warn("Pega3 VERIFY sin comprobante: no hay URL configurada para wsKey={}",
                    toWsKey(VERIFY_COMPROBANTE_KEY, serviceDefinition.getMovementType()));
            return null;
        }

        Pega3ComprobanteQueryCommand comprobanteCommand = Pega3ComprobanteQueryCommand.builder()
                .uuid(request.getUuid())
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode())
                .subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode())
                .rmsItemCode(request.getRmsItemCode())
                .ventaId(ventaId)
                .idUsuario(provider.getAuth().getLogin().getUsername())
                .transaccion(transaccion)
                .puntoDeVenta(request.getStoreName())
                .build();

        ExternalTransactionResponse comprobanteResponse = pega3ComprobanteQueryPort.generarComprobante(comprobanteCommand, comprobanteUrl);
        if (!comprobanteResponse.isApproved()) {
            return null;
        }
        Object rawImagenes = comprobanteResponse.getPayload() != null ? comprobanteResponse.getPayload().get("imagenes") : null;
        if (!(rawImagenes instanceof List<?> list)) {
            return null;
        }
        return list.stream()
                .filter(i -> i instanceof com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3ComprobanteResponse.Imagen)
                .map(i -> (com.omnistack.backend.infrastructure.adapter.integration.pega3.dto.Pega3ComprobanteResponse.Imagen) i)
                .map(img -> new ComprobanteData(img.getBase64(), img.getContentType()))
                .collect(java.util.stream.Collectors.toList());
    }

    private VerifyResponse buildResponse(BaseTransactionRequest request, ExternalTransactionResponse externalResponse, List<ComprobanteData> comprobantes) {
        Map<String, Object> payload = externalResponse.getPayload();
        boolean isError = !externalResponse.isApproved();

        List<String> comprobanteUrls = comprobantes != null
                ? comprobantes.stream()
                        .map(c -> comprobanteUrlService.storeAndBuildUrl(c.base64(), c.contentType()))
                        .collect(java.util.stream.Collectors.toList())
                : null;

        VerifyResponse.VerifyResponseBuilder<?, ?> builder = VerifyResponse.builder()
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .uuid(request.getUuid())
                .categoryCode(request.getCategoryCode())
                .subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode())
                .rmsItemCode(request.getRmsItemCode())
                .errorFlag(isError)
                .authorization(stringValue(payload, "authorization"))
                .ticketNumber(stringValue(payload, "ticket_number"))
                .ticketStatus(stringValue(payload, "ticket_status"))
                .winner(getBooleanValue(payload, "is_winner"))
                .prizeAmount(decimalValue(payload, "prize_amount"))
                .comprobanteUrls(comprobanteUrls);

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(CanonicalErrorCodeMapper.resolve(externalResponse))
                    .message(externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.status(new StatusDetail(StatusCodes.SUCCESS, "Consulta ticket Pega3 completada"));
        }

        return builder.build();
    }

    private Boolean getBooleanValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value != null) {
            return Boolean.valueOf(String.valueOf(value));
        }
        return null;
    }
}

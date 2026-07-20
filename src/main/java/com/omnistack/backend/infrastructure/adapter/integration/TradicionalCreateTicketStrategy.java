package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.CreateTicketRequest;
import com.omnistack.backend.application.dto.CreateTicketResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.TradicionalNumerosQueryPort;
import com.omnistack.backend.application.port.out.TradicionalSorteosQueryPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.CreateTicketStrategy;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.domain.model.TradicionalNumerosQueryCommand;
import com.omnistack.backend.domain.model.TradicionalSorteosQueryCommand;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalNumerosQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalSorteosQueryResponse;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia CREATE_TICKET CASH_IN para LN Tradicionales. Llama a
 * RecuperarNumerosDisponiblesPorCombinacion — el proveedor documenta esta
 * operacion como "obtener Y RESERVAR" combinaciones, por eso vive en
 * CREATE_TICKET (que ya tiene semantica de reserva, como en Pega3) y no en
 * PRECHECK (que es prevalidacion sin efectos secundarios).
 * <p>
 * Pozo Millonario + Revancha: se llama SIEMPRE con el juegoId principal (5)
 * solamente — el proveedor confirmo (spec + reunion directa) que una unica
 * llamada con juegoId=5 ya devuelve ambos juegos anidados en el mismo
 * listaDetalle (entradas juegoId=5 y juegoId=17 asociadas por "boleto").
 * NO se debe volver a llamar queryNumeros con juegoId=17: como este endpoint
 * reserva (efecto secundario), una segunda llamada generaria una reserva
 * separada y descartada (numeroReserva distinto que nunca se reenvia a
 * VentaBoletos). querySorteos si se llama para ambos juegos, pero solo para
 * obtener el pvp de cada uno (RecuperarSorteosDisponibles es de solo lectura).
 */
@Component
@RequiredArgsConstructor
public class TradicionalCreateTicketStrategy extends AbstractProviderStrategy implements CreateTicketStrategy {

    private static final String PROVIDER_KEY = "tradicional";
    private static final String PROVIDER_NAME = "Loteria Tradicionales";
    private static final String PRECHECK_SORTEOS_KEY = "PRECHECK_SORTEOS";
    private static final String JUEGO_ID_FIELD_PREFIX = "juego_id";

    private final TradicionalSorteosQueryPort sorteosQueryPort;
    private final TradicionalNumerosQueryPort numerosQueryPort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.CREATE_TICKET
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

        if (!(request instanceof CreateTicketRequest createTicketRequest)) {
            throw new IntegrationException("Tradicionales CREATE_TICKET requiere un CreateTicketRequest");
        }
        if (createTicketRequest.getDrawId() == null || createTicketRequest.getDrawId().isBlank()) {
            throw new IntegrationException("Tradicionales requiere draw_id para buscar/reservar combinaciones");
        }

        String numerosUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);
        String sorteosUrl = providerWsService.findUrl(PROVIDER_KEY, toWsKey(PRECHECK_SORTEOS_KEY, serviceDefinition.getMovementType())).orElse(null);

        String juegoId = resolveItemDefault(
                null, providerWsDefsService, PROVIDER_KEY,
                toWsKey(capability.name(), serviceDefinition.getMovementType()),
                JUEGO_ID_FIELD_PREFIX, request.getRmsItemCode(), PROVIDER_NAME);

        String drawId = createTicketRequest.getDrawId();
        String combinacion = createTicketRequest.getCombinacion();
        Boolean sugerir = createTicketRequest.getSugerir();
        Integer registros = createTicketRequest.getRegistros();
        String figuraId = createTicketRequest.getFiguraId();
        Integer cantidadFracciones = createTicketRequest.getCantidadFracciones() != null
                ? createTicketRequest.getCantidadFracciones() : 0;

        // Unica llamada de reserva — con juegoId=5, el proveedor ya anida las
        // entradas de Revancha (juegoId=17) en el mismo listaDetalle si el
        // sorteo la tiene. NO se vuelve a llamar queryNumeros con juegoId=17.
        ExternalTransactionResponse numerosResponse = queryNumeros(
                request, provider, juegoId, drawId, combinacion, figuraId, sugerir, registros, cantidadFracciones, numerosUrl);

        SorteoPricing mainPricing = null;
        String revanchaJuegoId = null;
        SorteoPricing revanchaPricing = null;
        if (sorteosUrl != null && !sorteosUrl.isBlank()) {
            ExternalTransactionResponse sorteosResponse = querySorteos(request, provider, juegoId, sorteosUrl);
            mainPricing = findSorteoPricing(sorteosResponse, drawId);
            RevanchaInfo revanchaInfo = findRevanchaInfo(sorteosResponse, drawId);
            if (revanchaInfo != null) {
                revanchaJuegoId = revanchaInfo.juegoRevanchaId();
                // Solo lectura (RecuperarSorteosDisponibles) — se llama para obtener el pvp
                // propio de la Revancha, que puede ser distinto al de Pozo Millonario.
                ExternalTransactionResponse revanchaSorteosResponse = querySorteos(
                        request, provider, revanchaInfo.juegoRevanchaId(), sorteosUrl);
                revanchaPricing = findSorteoPricing(revanchaSorteosResponse, revanchaInfo.sorteoRevanchaId());
            }
        }

        return buildResponse(request, numerosResponse, mainPricing, revanchaJuegoId, revanchaPricing);
    }

    private ExternalTransactionResponse querySorteos(
            BaseTransactionRequest request, AppProperties.ProviderProperties provider, String juegoId, String sorteosUrl) {
        TradicionalSorteosQueryCommand sorteosCmd = TradicionalSorteosQueryCommand.builder()
                .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                .storeName(request.getStoreName()).pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .medioId(provider.getMedioId()).userName(provider.getAuth().getLogin().getUsername())
                .juegoId(juegoId)
                .build();
        return sorteosQueryPort.querySorteos(sorteosCmd, sorteosUrl);
    }

    private ExternalTransactionResponse queryNumeros(
            BaseTransactionRequest request, AppProperties.ProviderProperties provider,
            String juegoId, String sorteoId, String combinacion, String figuraId,
            Boolean sugerir, Integer registros, Integer cantidadFracciones, String numerosUrl) {
        TradicionalNumerosQueryCommand numerosCmd = TradicionalNumerosQueryCommand.builder()
                .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                .storeName(request.getStoreName()).pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .medioId(provider.getMedioId()).userName(provider.getAuth().getLogin().getUsername())
                .juegoId(juegoId).sorteoId(sorteoId)
                .combinacion(combinacion != null ? combinacion : "")
                .combinacionFigura(figuraId != null ? figuraId : "")
                .sugerir(sugerir != null ? sugerir : false)
                .cantidad(cantidadFracciones)
                .registros(registros != null ? registros : 10)
                .build();
        return numerosQueryPort.queryNumeros(numerosCmd, numerosUrl);
    }

    private record RevanchaInfo(String juegoRevanchaId, String sorteoRevanchaId) {
    }

    /** pvp = precio de venta al publico por unidad/fraccion (confirmado en el spec: respuesta de
     * VentaBoletos, "Valor: Decimal — Valor total (pvp*cantidad)", es multiplicacion directa sin
     * dividir). cantidadFraccion = cuantas fracciones componen un entero (0/null en juegos sin
     * fraccionamiento, ej. Pozo Millonario) — solo informativo, no participa en el precio. */
    private record SorteoPricing(java.math.BigDecimal pvp, Integer cantidadFraccion) {
    }

    @SuppressWarnings("unchecked")
    private SorteoPricing findSorteoPricing(ExternalTransactionResponse sorteosResponse, String sorteoId) {
        if (sorteosResponse == null || !sorteosResponse.isApproved() || sorteosResponse.getPayload() == null) {
            return null;
        }
        Object rawSorteos = sorteosResponse.getPayload().get("listaSorteos");
        if (!(rawSorteos instanceof List<?> list)) {
            return null;
        }
        return list.stream()
                .filter(s -> s instanceof TradicionalSorteosQueryResponse.Sorteo)
                .map(s -> (TradicionalSorteosQueryResponse.Sorteo) s)
                .filter(sorteo -> sorteoId.equals(sorteo.getSorteoId()))
                .findFirst()
                .map(sorteo -> new SorteoPricing(sorteo.getPrecio(), sorteo.getCantidadFraccion()))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private RevanchaInfo findRevanchaInfo(ExternalTransactionResponse sorteosResponse, String drawId) {
        if (sorteosResponse == null || !sorteosResponse.isApproved() || sorteosResponse.getPayload() == null) {
            return null;
        }
        Object rawSorteos = sorteosResponse.getPayload().get("listaSorteos");
        if (!(rawSorteos instanceof List<?> list)) {
            return null;
        }
        return list.stream()
                .filter(s -> s instanceof TradicionalSorteosQueryResponse.Sorteo)
                .map(s -> (TradicionalSorteosQueryResponse.Sorteo) s)
                .filter(sorteo -> drawId.equals(sorteo.getSorteoId()))
                .filter(sorteo -> Boolean.TRUE.equals(sorteo.getTieneRevancha())
                        && sorteo.getJuegoRevanchaId() != null && sorteo.getSorteoRevanchaId() != null)
                .findFirst()
                .map(sorteo -> new RevanchaInfo(sorteo.getJuegoRevanchaId(), sorteo.getSorteoRevanchaId()))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<CreateTicketResponse.TradicionalNumber> mapNumeros(
            ExternalTransactionResponse numerosResp,
            SorteoPricing mainPricing,
            String revanchaJuegoId,
            SorteoPricing revanchaPricing) {
        if (numerosResp == null || !numerosResp.isApproved() || numerosResp.getPayload() == null) {
            return null;
        }
        Object rawNumeros = numerosResp.getPayload().get("listaNumeros");
        if (!(rawNumeros instanceof List<?> list)) {
            return null;
        }
        return list.stream()
                .filter(n -> n instanceof TradicionalNumerosQueryResponse.Numero)
                .map(n -> {
                    TradicionalNumerosQueryResponse.Numero num = (TradicionalNumerosQueryResponse.Numero) n;
                    // Cada entrada del listaDetalle ya trae su propio juegoId (5 o 17, cuando
                    // aplica Revancha) — el precio unitario se elige por entrada, no por lista.
                    SorteoPricing pricing = revanchaJuegoId != null && revanchaJuegoId.equals(num.getJuegoId())
                            ? revanchaPricing
                            : mainPricing;
                    return CreateTicketResponse.TradicionalNumber.builder()
                            .numero(num.getNumero())
                            .numero2(num.getNumero2())
                            .numero3(num.getNumero3())
                            .numero4(num.getNumero4())
                            .numero5(num.getNumero5())
                            .figura(num.getFigura())
                            .juegoId(num.getJuegoId())
                            .sorteoId(num.getSorteoId())
                            .boleto(num.getBoleto())
                            .fracciones(num.getFracciones())
                            .cantidad(num.getCantidad())
                            .reserva(num.getReserva())
                            .precioUnitario(pricing != null ? pricing.pvp() : null)
                            .build();
                }).collect(Collectors.toList());
    }

    private CreateTicketResponse buildResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse numerosResp,
            SorteoPricing mainPricing,
            String revanchaJuegoId,
            SorteoPricing revanchaPricing) {

        boolean isError = numerosResp == null || !numerosResp.isApproved();

        List<CreateTicketResponse.TradicionalNumber> availableNumbers = mapNumeros(numerosResp, mainPricing, revanchaJuegoId, revanchaPricing);
        Integer totalNumbers = numerosResp != null && numerosResp.getPayload() != null
                && numerosResp.getPayload().get("totalResults") instanceof Integer intTotal
                ? intTotal : null;
        String reservaId = numerosResp != null ? stringValue(numerosResp.getPayload(), "numeroReserva") : null;

        CreateTicketResponse.CreateTicketResponseBuilder<?, ?> builder = CreateTicketResponse.builder()
                .chain(request.getChain()).store(request.getStore()).storeName(request.getStoreName())
                .pos(request.getPos()).channelPos(request.getChannelPos().name())
                .uuid(request.getUuid())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .errorFlag(isError)
                .availableNumbers(availableNumbers).totalNumbers(totalNumbers).reservaId(reservaId);

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(numerosResp != null ? numerosResp.getExternalCode() : "ERROR")
                    .message(numerosResp != null ? numerosResp.getExternalMessage() : "Sin respuesta del proveedor")
                    .build());
        } else {
            builder.status(new StatusDetail(numerosResp.getExternalCode(), "Busqueda/reserva de combinaciones Tradicionales completada"));
        }

        return builder.build();
    }

}

package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.TradicionalFigurasQueryPort;
import com.omnistack.backend.application.port.out.TradicionalJuegoQueryPort;
import com.omnistack.backend.application.port.out.TradicionalSorteosQueryPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.domain.model.TradicionalFigurasQueryCommand;
import com.omnistack.backend.domain.model.TradicionalJuegoQueryCommand;
import com.omnistack.backend.domain.model.TradicionalSorteosQueryCommand;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalFigurasQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalSorteosQueryResponse;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia PRECHECK CASH_IN para LN Tradicionales. Es prevalidacion pura,
 * sin efectos secundarios en el proveedor: RecuperarJuegosPorMedio,
 * RecuperarSorteosDisponibles, RecuperarFigurasPorJuego. La busqueda y
 * reserva de combinaciones (RecuperarNumerosDisponiblesPorCombinacion, que
 * el proveedor documenta como "obtener Y RESERVAR") vive en
 * {@link TradicionalCreateTicketStrategy} porque tiene efecto secundario —
 * no corresponde a prevalidacion.
 */
@Component
@RequiredArgsConstructor
public class LoteriaTradicionalPrecheckStrategy extends AbstractProviderStrategy implements PrecheckStrategy {

    private static final String PROVIDER_KEY = "tradicional";
    private static final String PROVIDER_NAME = "Loteria Tradicionales";
    private static final String PRECHECK_SORTEOS_KEY = "PRECHECK_SORTEOS";
    private static final String PRECHECK_FIGURAS_KEY = "PRECHECK_FIGURAS";
    private static final String JUEGO_ID_FIELD_PREFIX = "juego_id";
    private static final String JUEGO_ID_POZO_MILLONARIO = "5";

    private final TradicionalJuegoQueryPort juegoQueryPort;
    private final TradicionalSorteosQueryPort sorteosQueryPort;
    private final TradicionalFigurasQueryPort figurasQueryPort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.PRECHECK
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
        validateBusinessContext(request, serviceDefinition, provider);

        String juegosUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);
        String sorteosUrl = providerWsService.findUrl(PROVIDER_KEY, toWsKey(PRECHECK_SORTEOS_KEY, serviceDefinition.getMovementType())).orElse(null);
        String figurasUrl = providerWsService.findUrl(PROVIDER_KEY, toWsKey(PRECHECK_FIGURAS_KEY, serviceDefinition.getMovementType())).orElse(null);

        String gameId = request instanceof PrecheckRequest precheckRequest ? precheckRequest.getGameId() : null;
        String juegoId = resolveItemDefault(
                gameId, providerWsDefsService, PROVIDER_KEY,
                toWsKey(capability.name(), serviceDefinition.getMovementType()),
                JUEGO_ID_FIELD_PREFIX, request.getRmsItemCode(), PROVIDER_NAME);

        // Call 1: RecuperarJuegosPorMedio (always)
        TradicionalJuegoQueryCommand juegoCmd = TradicionalJuegoQueryCommand.builder()
                .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                .storeName(request.getStoreName()).pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .medioId(provider.getMedioId()).userName(provider.getAuth().getLogin().getUsername())
                .build();

        ExternalTransactionResponse juegosResponse = juegoQueryPort.queryJuegos(juegoCmd, juegosUrl);

        // Call 2: RecuperarSorteosDisponibles (if operation configured)
        ExternalTransactionResponse sorteosResponse = null;
        if (sorteosUrl != null && !sorteosUrl.isBlank()) {
            TradicionalSorteosQueryCommand sorteosCmd = TradicionalSorteosQueryCommand.builder()
                    .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                    .storeName(request.getStoreName()).pos(request.getPos())
                    .channelPos(request.getChannelPos().name())
                    .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                    .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                    .medioId(provider.getMedioId()).userName(provider.getAuth().getLogin().getUsername())
                    .juegoId(juegoId)
                    .build();
            sorteosResponse = sorteosQueryPort.querySorteos(sorteosCmd, sorteosUrl);
        }

        // Call 3: RecuperarFigurasPorJuego (if operation configured)
        ExternalTransactionResponse figurasResponse = null;
        if (figurasUrl != null && !figurasUrl.isBlank()) {
            TradicionalFigurasQueryCommand figurasCmd = TradicionalFigurasQueryCommand.builder()
                    .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                    .storeName(request.getStoreName()).pos(request.getPos())
                    .channelPos(request.getChannelPos().name())
                    .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                    .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                    .medioId(provider.getMedioId()).userName(provider.getAuth().getLogin().getUsername())
                    .juegoId(juegoId)
                    .build();
            figurasResponse = figurasQueryPort.queryFiguras(figurasCmd, figurasUrl);
        }

        return buildResponse(request, juegoId, juegosResponse, sorteosResponse, figurasResponse);
    }

    @SuppressWarnings("unchecked")
    private PrecheckResponse buildResponse(
            BaseTransactionRequest request,
            String juegoId,
            ExternalTransactionResponse juegosResp,
            ExternalTransactionResponse sorteosResp,
            ExternalTransactionResponse figurasResp) {

        boolean isError = !juegosResp.isApproved();

        // Build draws list
        List<PrecheckResponse.TradicionalDraw> draws = null;
        if (sorteosResp != null && sorteosResp.isApproved() && sorteosResp.getPayload() != null) {
            Object rawSorteos = sorteosResp.getPayload().get("listaSorteos");
            if (rawSorteos instanceof List<?> list) {
                draws = list.stream()
                        .filter(s -> s instanceof TradicionalSorteosQueryResponse.Sorteo)
                        .map(s -> {
                            TradicionalSorteosQueryResponse.Sorteo sorteo = (TradicionalSorteosQueryResponse.Sorteo) s;
                            return PrecheckResponse.TradicionalDraw.builder()
                                    .drawId(sorteo.getSorteoId()).nombre(sorteo.getNombre())
                                    .fecha(sorteo.getFecha()).precio(sorteo.getPrecio())
                                    .premioMayor(sorteo.getPremioMayor())
                                    .cantidadFraccion(sorteo.getCantidadFraccion())
                                    .tieneRevancha(sorteo.getTieneRevancha())
                                    .juegoRevanchaId(sorteo.getJuegoRevanchaId())
                                    .sorteoRevanchaId(sorteo.getSorteoRevanchaId())
                                    .build();
                        }).collect(java.util.stream.Collectors.toList());
            }
        }

        // Build figures list
        List<PrecheckResponse.TradicionalFigura> figures = null;
        if (figurasResp != null && figurasResp.isApproved() && figurasResp.getPayload() != null) {
            Object rawFiguras = figurasResp.getPayload().get("listaFiguras");
            if (rawFiguras instanceof List<?> list) {
                figures = list.stream()
                        .filter(f -> f instanceof TradicionalFigurasQueryResponse.Figura)
                        .map(f -> {
                            TradicionalFigurasQueryResponse.Figura figura = (TradicionalFigurasQueryResponse.Figura) f;
                            return PrecheckResponse.TradicionalFigura.builder()
                                    .figuraId(figura.getFiguraId()).nombre(figura.getNombre())
                                    .descripcion(figura.getDescripcion())
                                    .build();
                        }).collect(java.util.stream.Collectors.toList());
            }
        }

        // El proveedor puede responder codError=0 (sin error tecnico) pero sin datos utiles
        // (listaDetalle=null) — ej. sin sorteos abiertos para venta en este momento. Sin sorteos
        // no hay como continuar el flujo para ningun juego; sin figuras, Pozo Millonario no puede
        // completar la seleccion de mascota (RN-05/RF-06 de negocio).
        if (!isError) {
            if (draws == null || draws.isEmpty()) {
                throw new IntegrationException(
                        "Loteria Nacional no tiene sorteos disponibles para el juego " + juegoId + " en este momento");
            }
            if (JUEGO_ID_POZO_MILLONARIO.equals(juegoId) && (figures == null || figures.isEmpty())) {
                throw new IntegrationException(
                        "Loteria Nacional no tiene figuras/mascotas disponibles para Pozo Millonario en este momento");
            }
        }

        PrecheckResponse.PrecheckResponseBuilder<?, ?> builder = PrecheckResponse.builder()
                .chain(request.getChain()).store(request.getStore()).storeName(request.getStoreName())
                .pos(request.getPos()).channelPos(request.getChannelPos().name())
                .uuid(request.getUuid())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .errorFlag(isError)
                .draws(draws).figures(figures);

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(juegosResp.getExternalCode())
                    .message(juegosResp.getExternalMessage())
                    .build());
        } else {
            builder.status(new StatusDetail(juegosResp.getExternalCode(), "Precheck Tradicionales completado"));
        }

        return builder.build();
    }

    private void validateBusinessContext(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            AppProperties.ProviderProperties provider) {
        validateValue("category_code", request.getCategoryCode(), provider.getCategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
        validateValue("category_code", serviceDefinition.getCategoryCode(), provider.getCategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
    }
}

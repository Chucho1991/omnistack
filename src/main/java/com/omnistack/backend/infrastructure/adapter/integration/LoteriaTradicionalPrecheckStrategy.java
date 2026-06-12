package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.TradicionalFigurasQueryPort;
import com.omnistack.backend.application.port.out.TradicionalJuegoQueryPort;
import com.omnistack.backend.application.port.out.TradicionalNumerosQueryPort;
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
import com.omnistack.backend.domain.model.TradicionalNumerosQueryCommand;
import com.omnistack.backend.domain.model.TradicionalSorteosQueryCommand;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalFigurasQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalJuegoQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalNumerosQueryResponse;
import com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto.TradicionalSorteosQueryResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia PRECHECK CASH_IN para LN Tradicionales. Realiza hasta 4 llamadas:
 * RecuperarJuegosPorMedio, RecuperarSorteosDisponibles, RecuperarFigurasPorJuego,
 * RecuperarNumerosDisponiblesPorCombinacion.
 */
@Component
@RequiredArgsConstructor
public class LoteriaTradicionalPrecheckStrategy extends AbstractProviderStrategy implements PrecheckStrategy {

    private static final String PROVIDER_KEY = "tradicional";
    private static final String PROVIDER_NAME = "Loteria Tradicionales";
    private static final String PRECHECK_SORTEOS_KEY = "PRECHECK_SORTEOS";
    private static final String PRECHECK_FIGURAS_KEY = "PRECHECK_FIGURAS";
    private static final String PRECHECK_NUMEROS_KEY = "PRECHECK_NUMEROS";
    private static final String DEFAULT_JUEGO_ID = "1";

    private final TradicionalJuegoQueryPort juegoQueryPort;
    private final TradicionalSorteosQueryPort sorteosQueryPort;
    private final TradicionalFigurasQueryPort figurasQueryPort;
    private final TradicionalNumerosQueryPort numerosQueryPort;
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
                && serviceDefinition.getSubcategoryCode() != null
                && serviceDefinition.getSubcategoryCode().equalsIgnoreCase(provider.getSubcategoryCode())
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
        String numerosUrl = providerWsService.findUrl(PROVIDER_KEY, toWsKey(PRECHECK_NUMEROS_KEY, serviceDefinition.getMovementType())).orElse(null);

        String gameId = null;
        String drawId = null;
        String combinacion = null;
        Boolean sugerir = null;
        Integer registros = null;
        if (request instanceof PrecheckRequest precheckRequest) {
            gameId = precheckRequest.getGameId();
            drawId = precheckRequest.getDrawId();
            combinacion = precheckRequest.getCombinacion();
            sugerir = precheckRequest.getSugerir();
            registros = precheckRequest.getRegistros();
        }
        String juegoId = gameId != null ? gameId : DEFAULT_JUEGO_ID;

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

        // Call 4: RecuperarNumerosDisponiblesPorCombinacion (only if drawId is provided)
        ExternalTransactionResponse numerosResponse = null;
        if (numerosUrl != null && !numerosUrl.isBlank()
                && drawId != null && !drawId.isBlank()) {
            TradicionalNumerosQueryCommand numerosCmd = TradicionalNumerosQueryCommand.builder()
                    .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                    .storeName(request.getStoreName()).pos(request.getPos())
                    .channelPos(request.getChannelPos().name())
                    .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                    .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                    .medioId(provider.getMedioId()).userName(provider.getAuth().getLogin().getUsername())
                    .juegoId(juegoId).sorteoId(drawId)
                    .combinacion(combinacion != null ? combinacion : "")
                    .combinacionFigura("")
                    .sugerir(sugerir != null ? sugerir : false)
                    .cantidad(0)
                    .registros(registros != null ? registros : 10)
                    .build();
            numerosResponse = numerosQueryPort.queryNumeros(numerosCmd, numerosUrl);
        }

        return buildResponse(request, juegosResponse, sorteosResponse, figurasResponse, numerosResponse);
    }

    @SuppressWarnings("unchecked")
    private PrecheckResponse buildResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse juegosResp,
            ExternalTransactionResponse sorteosResp,
            ExternalTransactionResponse figurasResp,
            ExternalTransactionResponse numerosResp) {

        boolean isError = !juegosResp.isApproved();

        // Build games list
        List<PrecheckResponse.TradicionalGame> games = null;
        if (juegosResp.isApproved() && juegosResp.getPayload() != null) {
            Object rawJuegos = juegosResp.getPayload().get("juegos");
            if (rawJuegos instanceof List<?> list) {
                games = list.stream()
                        .filter(j -> j instanceof TradicionalJuegoQueryResponse)
                        .map(j -> {
                            TradicionalJuegoQueryResponse jg = (TradicionalJuegoQueryResponse) j;
                            return PrecheckResponse.TradicionalGame.builder()
                                    .gameId(jg.getJuegoId()).nombre(jg.getNombre()).descripcion(jg.getDescripcion())
                                    .build();
                        }).collect(java.util.stream.Collectors.toList());
            }
        }

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
                                    .premioMayor(sorteo.getPremioMayor()).disponible(sorteo.getDisponible())
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

        // Build available numbers list
        List<PrecheckResponse.TradicionalNumber> availableNumbers = null;
        Integer totalNumbers = null;
        if (numerosResp != null && numerosResp.isApproved() && numerosResp.getPayload() != null) {
            Object rawNumeros = numerosResp.getPayload().get("listaNumeros");
            Object rawTotal = numerosResp.getPayload().get("totalResults");
            if (rawTotal instanceof Integer intTotal) {
                totalNumbers = intTotal;
            }
            if (rawNumeros instanceof List<?> list) {
                availableNumbers = list.stream()
                        .filter(n -> n instanceof TradicionalNumerosQueryResponse.Numero)
                        .map(n -> {
                            TradicionalNumerosQueryResponse.Numero num = (TradicionalNumerosQueryResponse.Numero) n;
                            return PrecheckResponse.TradicionalNumber.builder()
                                    .numero(num.getNumero()).disponible(num.getDisponible())
                                    .precio(num.getPrecio())
                                    .build();
                        }).collect(java.util.stream.Collectors.toList());
            }
        }

        PrecheckResponse.PrecheckResponseBuilder<?, ?> builder = PrecheckResponse.builder()
                .chain(request.getChain()).store(request.getStore()).storeName(request.getStoreName())
                .pos(request.getPos()).channelPos(request.getChannelPos().name())
                .uuid(request.getUuid())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .errorFlag(isError)
                .games(games).draws(draws).figures(figures)
                .availableNumbers(availableNumbers).totalNumbers(totalNumbers);

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
        validateValue("subcategory_code", request.getSubcategoryCode(), provider.getSubcategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
        validateValue("category_code", serviceDefinition.getCategoryCode(), provider.getCategoryCode(), PROVIDER_NAME);
        validateValue("subcategory_code", serviceDefinition.getSubcategoryCode(), provider.getSubcategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
    }
}

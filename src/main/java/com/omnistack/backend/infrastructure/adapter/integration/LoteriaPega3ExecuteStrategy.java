package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.ExecuteStrategy;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia de EXECUTE CASH_IN para Pega3.
 * <p>
 * CrearTicket (llamado en CREATE_TICKET) ya vende el ticket por completo — el proveedor
 * documenta su respuesta con "status":"Purchased". No existe un endpoint separado de
 * "confirmar venta" para Pega3 (a diferencia de Tradicionales, que reserva en un paso y
 * vende en otro con VentaBoletos). Por eso EXECUTE aqui NO debe llamar a PagarTicket — ese
 * endpoint es para COBRAR el premio de un ticket YA vendido y ganador (se usa en CASH_OUT,
 * ver {@link LoteriaPega3CashOutExecuteStrategy}); llamarlo con un ticket recien creado
 * produce "Invalid Barcode" porque el proveedor lo interpreta como intento de cobro de un
 * ticket que no es ganador. EXECUTE solo confirma, usando el ticketNumber que el POS
 * reenvia desde CREATE_TICKET.
 */
@Component
@RequiredArgsConstructor
public class LoteriaPega3ExecuteStrategy extends AbstractProviderStrategy implements ExecuteStrategy {

    private static final String PROVIDER_KEY = "pega3";
    private static final String PROVIDER_NAME = "Loteria Pega3";

    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.EXECUTE
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

        if (request.getAuthorization() == null || request.getAuthorization().isBlank()) {
            throw new IntegrationException("Pega3 requiere authorization (ticketNumber, devuelto por CREATE_TICKET) para EXECUTE");
        }
        if (request.getAmount() == null) {
            throw new IntegrationException("Pega3 requiere amount para EXECUTE");
        }

        return ExecuteResponse.builder()
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
                .errorFlag(false)
                .authorization(request.getAuthorization())
                .amount(request.getAmount())
                .status(new StatusDetail(StatusCodes.SUCCESS, "Venta de ticket Pega3 confirmada"))
                .build();
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

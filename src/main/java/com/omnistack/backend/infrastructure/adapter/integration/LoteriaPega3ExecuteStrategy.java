package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.Pega3CreateTicketPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.ExecuteStrategy;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3CreateTicketCommand;
import com.omnistack.backend.domain.model.Pega3Panel;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia de EXECUTE CASH_IN para Pega3. Llama a CrearTicket.
 * <p>
 * CrearTicket vende el ticket por completo — el proveedor documenta su respuesta con
 * "status":"Purchased". No existe un endpoint separado de "confirmar venta" ni de "reservar"
 * para Pega3 (a diferencia de Tradicionales, que reserva en CREATE_TICKET y vende en otro
 * paso con VentaBoletos) — por eso ya no hay CREATE_TICKET para este proveedor, todo el
 * flujo de venta ocurre aqui, en EXECUTE. El WS_KEY "EXECUTE.CASHIN" en
 * IN_OMNI_PROVEEDOR_WS apunta a CrearTicket (reapuntado desde PagarTicket, que era el
 * endpoint equivocado usado antes de este cambio — PagarTicket es para CASH_OUT, ver
 * {@link LoteriaPega3CashOutExecuteStrategy}).
 */
@Component
@RequiredArgsConstructor
public class LoteriaPega3ExecuteStrategy extends AbstractProviderStrategy implements ExecuteStrategy {

    private static final String PROVIDER_KEY = "pega3";
    private static final String PROVIDER_NAME = "Loteria Pega3";

    private final Pega3CreateTicketPort pega3CreateTicketPort;
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
        if (!(request instanceof ExecuteRequest executeRequest)) {
            throw new IntegrationException("Pega3 EXECUTE requiere un ExecuteRequest");
        }
        if (executeRequest.getTicketData() == null) {
            throw new IntegrationException("Pega3 requiere ticket_data para vender el ticket");
        }

        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);

        List<Pega3Panel> panels = buildDomainPanels(executeRequest.getTicketData().getPanels());

        Pega3CreateTicketCommand command = Pega3CreateTicketCommand.builder()
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
                .amount(executeRequest.getAmount())
                .drawNumber(executeRequest.getTicketData().getDrawNumber())
                .entryType(executeRequest.getTicketData().getEntryType())
                .panels(panels)
                .build();

        ExternalTransactionResponse externalResponse = pega3CreateTicketPort.createTicket(command, operationUrl);
        return buildResponse(request, externalResponse);
    }

    private ExecuteResponse buildResponse(BaseTransactionRequest request, ExternalTransactionResponse externalResponse) {
        Map<String, Object> payload = externalResponse.getPayload();
        boolean isError = !externalResponse.isApproved();

        ExecuteResponse.ExecuteResponseBuilder<?, ?> builder = ExecuteResponse.builder()
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
                .amount(request.getAmount())
                .ticketNumber(stringValue(payload, "ticket_number"))
                .ticketQr(stringValue(payload, "ticket_qr"));

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(CanonicalErrorCodeMapper.resolve(externalResponse))
                    .message(externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.status(new StatusDetail(StatusCodes.SUCCESS, "Venta de ticket Pega3 confirmada"));
        }

        return builder.build();
    }

    private List<Pega3Panel> buildDomainPanels(List<ExecuteRequest.TicketPanel> panels) {
        List<Pega3Panel> result = new ArrayList<>();
        if (panels == null) {
            return result;
        }
        for (ExecuteRequest.TicketPanel panel : panels) {
            result.add(Pega3Panel.builder()
                    .betAmount(panel.getBetAmount())
                    .numbers(panel.getNumbers())
                    .playTypes(panel.getPlayTypes())
                    .build());
        }
        return result;
    }
}

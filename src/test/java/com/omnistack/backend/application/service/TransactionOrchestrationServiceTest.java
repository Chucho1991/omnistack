package com.omnistack.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.ProviderFlowResolver;
import com.omnistack.backend.application.port.out.strategy.TransactionFlowStrategy;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ProviderFlowSelection;
import com.omnistack.backend.domain.model.ServiceDefinition;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TransactionOrchestrationServiceTest {

    @Test
    void shouldSetMovementTypeFromCatalogBeforeExecutingStrategy() {
        AtomicReference<BaseTransactionRequest> capturedRequest = new AtomicReference<>();
        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("10001565829")
                .movementType(MovementType.CASH_OUT)
                .capabilities(List.of(Capability.EXECUTE))
                .build();
        TransactionFlowStrategy strategy = new TransactionFlowStrategy() {
            @Override
            public boolean supports(ServiceDefinition definition, Capability capability) {
                return true;
            }

            @Override
            public BaseTransactionResponse process(
                    BaseTransactionRequest request,
                    ServiceDefinition definition,
                    Capability capability) {
                capturedRequest.set(request);
                return ExecuteResponse.builder()
                        .uuid(request.getUuid())
                        .errorFlag(false)
                        .status(new StatusDetail("0", "Transaccion correcta"))
                        .build();
            }
        };
        ProviderFlowResolver resolver = (request, capability) -> ProviderFlowSelection.builder()
                .serviceDefinition(serviceDefinition)
                .strategy(strategy)
                .build();
        TransactionOrchestrationService service = new TransactionOrchestrationService(
                resolver,
                new AuditLogService(log -> { }));

        ExecuteRequest request = ExecuteRequest.builder()
                .uuid("uuid-bet593-cashout")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("10001565829")
                .withdrawId("20240430800100007")
                .document("0911274165")
                .amount(new BigDecimal("17.00"))
                .build();

        assertNull(request.getMovementType());

        service.execute(request);

        assertEquals(MovementType.CASH_OUT, capturedRequest.get().getMovementType());
        assertEquals(MovementType.CASH_OUT, request.getMovementType());
    }
}

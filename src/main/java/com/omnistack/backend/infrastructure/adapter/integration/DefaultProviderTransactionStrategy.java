package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.mapper.ResponseFactory;
import com.omnistack.backend.application.port.out.ExternalProviderClient;
import com.omnistack.backend.application.port.out.strategy.ExecuteStrategy;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.application.port.out.strategy.ReverseStrategy;
import com.omnistack.backend.application.port.out.strategy.VerifyStrategy;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionRequest;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Estrategia default reusable para proveedores que compartan contrato base.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.integrations", name = "mock-enabled", havingValue = "true")
public class DefaultProviderTransactionStrategy
        implements PrecheckStrategy, ExecuteStrategy, VerifyStrategy, ReverseStrategy {

    private final ExternalProviderClient externalProviderClient;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        String providerCode = serviceDefinition.getServiceProviderCode();
        return providerCode != null
                && !"1".equalsIgnoreCase(providerCode)
                && !isImplementedBet593Recharge(serviceDefinition, capability);
    }

    private boolean isImplementedBet593Recharge(ServiceDefinition serviceDefinition, Capability capability) {
        return capability == Capability.PRECHECK
                && serviceDefinition.getMovementType() == MovementType.CASH_IN
                && "2".equalsIgnoreCase(serviceDefinition.getServiceProviderCode())
                && "10001565828".equalsIgnoreCase(serviceDefinition.getRmsItemCode());
    }

    @Override
    public BaseTransactionResponse process(BaseTransactionRequest request, ServiceDefinition serviceDefinition, Capability capability) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("chain", request.getChain());
        payload.put("store", request.getStore());
        payload.put("pos", request.getPos());
        payload.put("channel_POS", request.getChannelPos());
        payload.put("movement_type", serviceDefinition.getMovementType());
        if (capability != Capability.VERIFY) {
            payload.put("amount", request.getAmount());
        }
        payload.put("phone", request.getPhone());
        payload.put("document", request.getDocument());
        payload.put("userid", request.getUserid());
        payload.put("withdrawId", request.getWithdrawId());
        payload.put("authorization", request.getAuthorization());
        payload.put("serialnumber", request.getSerialnumber());
        payload.put("rms_item_code", request.getRmsItemCode());

        ExternalTransactionRequest externalRequest = ExternalTransactionRequest.builder()
                .uuid(request.getUuid())
                .providerCode(serviceDefinition.getServiceProviderCode())
                .capability(capability)
                .payload(payload)
                .build();

        ExternalTransactionResponse externalResponse = externalProviderClient.invoke(externalRequest);
        return ResponseFactory.transactionResponse(request, externalResponse, capability);
    }
}

package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.port.out.ExternalProviderClient;
import com.omnistack.backend.domain.model.ExternalTransactionRequest;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Cliente mockeable para simular integraciones externas REST.
 */
@Component
@ConditionalOnProperty(prefix = "app.integrations", name = "mock-enabled", havingValue = "true")
public class MockExternalProviderClient implements ExternalProviderClient {

    @Override
    public ExternalTransactionResponse invoke(ExternalTransactionRequest request) {
        return ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("OK")
                .externalMessage("Operacion aprobada por proveedor mock")
                .payload(Map.of(
                        "provider", request.getProviderCode(),
                        "capability", request.getCapability().name(),
                        "uuid", request.getUuid()))
                .build();
    }
}

package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.port.out.ProviderFlowResolver;
import com.omnistack.backend.application.port.out.strategy.TransactionFlowStrategy;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.model.ProviderFlowSelection;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.CatalogNotFoundException;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolver dinamico de estrategias de proveedor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultProviderFlowResolver implements ProviderFlowResolver {

    private final CatalogCacheService catalogCacheService;
    private final List<TransactionFlowStrategy> strategies;

    @Override
    public ProviderFlowSelection resolve(BaseTransactionRequest request, Capability capability) {
        ServiceDefinition serviceDefinition = catalogCacheService.getRequiredService(
                request.getCategoryCode(),
                request.getSubcategoryCode(),
                request.getServiceProviderCode(),
                request.getRmsItemCode());

        if (!serviceDefinition.getCapabilities().contains(capability)) {
            throw new CatalogNotFoundException("La capacidad " + capability + " no esta habilitada para el servicio");
        }

        TransactionFlowStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.supports(serviceDefinition, capability))
                .findFirst()
                .orElseThrow(() -> new IntegrationException("No existe estrategia configurada para el proveedor/capacidad"));

        log.info(
                "Provider flow resolved. capability={}, categoryCode={}, subcategoryCode={}, serviceProviderCode={}, rmsItemCode={}, movementType={}, strategy={}",
                capability,
                serviceDefinition.getCategoryCode(),
                serviceDefinition.getSubcategoryCode(),
                serviceDefinition.getServiceProviderCode(),
                serviceDefinition.getRmsItemCode(),
                serviceDefinition.getMovementType(),
                strategy.getClass().getSimpleName());

        return ProviderFlowSelection.builder()
                .serviceDefinition(serviceDefinition)
                .strategy(strategy)
                .build();
    }
}

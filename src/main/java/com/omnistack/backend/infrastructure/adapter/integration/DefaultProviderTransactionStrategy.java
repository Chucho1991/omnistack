package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.port.out.strategy.ExecuteStrategy;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.application.port.out.strategy.ReverseStrategy;
import com.omnistack.backend.application.port.out.strategy.VerifyStrategy;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
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

    /**
     * No declara soporte para servicios sin adaptador real configurado.
     *
     * @param serviceDefinition definicion del servicio seleccionada desde catalogo
     * @param capability capacidad solicitada
     * @return siempre {@code false} para forzar error de configuracion explicito
     */
    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        return false;
    }

    /**
     * Rechaza la ejecucion cuando no hay endpoint externo configurado.
     *
     * @param request request interno recibido por OMNISTACK
     * @param serviceDefinition definicion catalogada del servicio
     * @param capability capacidad solicitada
     * @return nunca retorna respuesta exitosa
     * @throws IntegrationException siempre que se intente usar el fallback sin configuracion
     */
    @Override
    public BaseTransactionResponse process(BaseTransactionRequest request, ServiceDefinition serviceDefinition, Capability capability) {
        throw new IntegrationException("No existe configuracion de endpoint externo para el proveedor/capacidad solicitados");
    }
}

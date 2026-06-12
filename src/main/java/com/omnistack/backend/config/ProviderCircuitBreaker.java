package com.omnistack.backend.config;

import com.omnistack.backend.shared.exception.IntegrationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Envuelve llamadas HTTP a proveedores externos con un circuit breaker por proveedor.
 * Si un proveedor acumula el 50% de fallos en una ventana de 10 llamadas,
 * el circuito se abre 30s y devuelve error inmediato sin esperar al timeout HTTP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderCircuitBreaker {

    private final CircuitBreakerRegistry registry;

    public <T> T execute(String providerKey, Supplier<T> call) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = registry.circuitBreaker(providerKey);
        try {
            return cb.executeSupplier(call);
        } catch (CallNotPermittedException e) {
            log.warn("Circuit abierto para proveedor={} — devolviendo error rapido", providerKey);
            throw new IntegrationException(
                    "Proveedor " + providerKey + " temporalmente no disponible. Intente nuevamente en unos segundos.");
        }
    }
}

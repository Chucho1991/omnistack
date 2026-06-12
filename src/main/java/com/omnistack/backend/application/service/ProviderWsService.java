package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.ProviderWsPort;
import com.omnistack.backend.shared.exception.IntegrationException;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicacion para resolucion de URLs de endpoints externos.
 * Carga el mapa completo de IN_OMNI_PROVEEDOR_WS al arrancar y sirve de cache en memoria.
 */
@Slf4j
@Service
public class ProviderWsService {

    private final ProviderWsPort wsPort;
    private Map<String, String> urlCache = Collections.emptyMap();

    public ProviderWsService(@Autowired(required = false) ProviderWsPort wsPort) {
        this.wsPort = wsPort;
    }

    @PostConstruct
    public void init() {
        if (wsPort != null) {
            urlCache = Collections.unmodifiableMap(wsPort.loadAll());
        } else {
            log.warn("ProviderWsPort no disponible — las URLs de proveedores no se cargaran desde DB");
        }
    }

    /**
     * Retorna la URL para el proveedor y clave de operacion dados.
     *
     * @param providerKey clave del proveedor (ej: "ecuabet", "loteria")
     * @param wsKey       clave de operacion (ej: "PRECHECK.CASHIN", "LOGIN")
     * @return URL completa si existe
     */
    public Optional<String> findUrl(String providerKey, String wsKey) {
        return Optional.ofNullable(urlCache.get(cacheKey(providerKey, wsKey)));
    }

    /**
     * Retorna la URL o lanza excepcion si no esta configurada.
     *
     * @param providerKey  clave del proveedor
     * @param wsKey        clave de operacion
     * @param providerName nombre legible del proveedor para el mensaje de error
     * @return URL completa
     * @throws IntegrationException si no existe URL para esa combinacion
     */
    public String requireUrl(String providerKey, String wsKey, String providerName) {
        return findUrl(providerKey, wsKey)
                .orElseThrow(() -> new IntegrationException(
                        providerName + " no tiene URL configurada en DB para " + wsKey));
    }

    /**
     * Indica si existe una URL habilitada para el proveedor y operacion dados.
     *
     * @param providerKey clave del proveedor
     * @param wsKey       clave de operacion
     * @return true si la URL existe
     */
    public boolean hasUrl(String providerKey, String wsKey) {
        return urlCache.containsKey(cacheKey(providerKey, wsKey));
    }

    private static String cacheKey(String providerKey, String wsKey) {
        return providerKey.toLowerCase() + "|" + wsKey.toUpperCase();
    }
}

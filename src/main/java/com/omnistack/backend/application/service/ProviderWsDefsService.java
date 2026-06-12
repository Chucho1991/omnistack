package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.ProviderWsDefsPort;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicacion para resolucion de valores por defecto de operaciones WS.
 * Carga el mapa completo de IN_OMNI_PROVEEDOR_WS_DEFS al arrancar y sirve de cache en memoria.
 * Clave de cache: "providerkey|WSKEY|field"
 */
@Slf4j
@Service
public class ProviderWsDefsService {

    private final ProviderWsDefsPort defsPort;
    private Map<String, String> cache = Collections.emptyMap();

    public ProviderWsDefsService(@Autowired(required = false) ProviderWsDefsPort defsPort) {
        this.defsPort = defsPort;
    }

    @PostConstruct
    public void init() {
        if (defsPort != null) {
            cache = Collections.unmodifiableMap(defsPort.loadAll());
        } else {
            log.warn("ProviderWsDefsPort no disponible — WS_DEFS no se cargaran desde DB");
        }
    }

    /**
     * Retorna el valor de texto para el proveedor, WS_KEY y campo dados.
     *
     * @param providerKey clave del proveedor (ej: "ecuabet", "loteria")
     * @param wsKey       clave de operacion (ej: "PRECHECK.CASHIN", "LOGIN")
     * @param field       nombre del campo (ej: "shop_id", "item", "medio_id")
     * @return valor como Optional
     */
    public Optional<String> findString(String providerKey, String wsKey, String field) {
        return Optional.ofNullable(cache.get(cacheKey(providerKey, wsKey, field)));
    }

    /**
     * Retorna el valor de texto o null si no existe.
     */
    public String getString(String providerKey, String wsKey, String field) {
        return cache.get(cacheKey(providerKey, wsKey, field));
    }

    /**
     * Retorna el valor numerico o null si no existe.
     */
    public Integer getInteger(String providerKey, String wsKey, String field) {
        String val = getString(providerKey, wsKey, field);
        if (val == null || val.isBlank()) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("ProviderWsDefs: valor no numerico para {}: {}", cacheKey(providerKey, wsKey, field), val);
            return null;
        }
    }

    /**
     * Indica si existe una entrada item.{rmsItemCode} en WS_DEFS para la operacion dada.
     * Soporta el formato multi-item: DEFAULT_CLAVE = "item.{rmsItemCode}".
     */
    public boolean hasItem(String providerKey, String wsKey, String rmsItemCode) {
        return cache.containsKey(cacheKey(providerKey, wsKey, "item." + rmsItemCode));
    }

    /**
     * Extrae todos los offer_id.{rmsItemCode} como Map&lt;rmsItemCode, offerId&gt;.
     * Las claves en WS_DEFS tienen formato "offer_id.{rmsItemCode}".
     */
    public Map<String, String> getOfferIds(String providerKey, String wsKey) {
        String prefix = cacheKey(providerKey, wsKey, "offer_id.");
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String rmsItemCode = entry.getKey().substring(prefix.length());
                result.put(rmsItemCode, entry.getValue());
            }
        }
        return result;
    }

    private static String cacheKey(String providerKey, String wsKey, String field) {
        return providerKey.toLowerCase() + "|" + wsKey.toUpperCase() + "|" + field.toLowerCase();
    }
}

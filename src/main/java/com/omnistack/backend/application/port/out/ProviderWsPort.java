package com.omnistack.backend.application.port.out;

import java.util.Map;
import java.util.Optional;

/**
 * Puerto de salida para consulta de URLs de endpoints externos de proveedores.
 * Los datos provienen de IN_OMNI_PROVEEDOR_WS en el schema GPF_OMNISTACK.
 */
public interface ProviderWsPort {

    /**
     * Carga todas las URLs habilitadas, indexadas por "providerKey|wsKey".
     *
     * @return mapa inmutable con todas las URLs activas
     */
    Map<String, String> loadAll();

    /**
     * Busca la URL para un proveedor y clave de operacion dados.
     *
     * @param providerKey clave del proveedor (ej: "ecuabet", "loteria", "pega3")
     * @param wsKey       clave de la operacion (ej: "PRECHECK.CASHIN", "LOGIN")
     * @return URL completa si existe y esta habilitada
     */
    Optional<String> findUrl(String providerKey, String wsKey);
}

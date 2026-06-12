package com.omnistack.backend.application.port.out;

import java.util.Map;

/**
 * Puerto de salida para carga de valores por defecto de operaciones WS.
 * Los datos provienen de IN_OMNI_PROVEEDOR_WS_DEFS en el schema GPF_OMNISTACK.
 */
public interface ProviderWsDefsPort {

    /**
     * Carga todos los defaults de WS, indexados por "providerKey|WSKEY|field".
     *
     * @return mapa inmutable con todas las entradas de defaults
     */
    Map<String, String> loadAll();
}

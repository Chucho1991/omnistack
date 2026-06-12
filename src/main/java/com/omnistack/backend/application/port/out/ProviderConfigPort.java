package com.omnistack.backend.application.port.out;

import java.util.Map;

/**
 * Puerto de salida para carga de configuracion funcional de proveedores.
 * Los datos provienen de IN_OMNI_PROVEEDOR_CONFIG en el schema GPF_OMNISTACK.
 */
public interface ProviderConfigPort {

    /**
     * Carga toda la config de proveedores, indexada por "providerKey|config_key".
     *
     * @return mapa inmutable con todas las entradas de configuracion
     */
    Map<String, String> loadAll();
}

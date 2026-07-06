package com.omnistack.backend.application.port.out;

import java.util.Map;

/**
 * Puerto de salida para carga de configuracion por item desde AD_ITEM_SERVICIO.
 * Clave del mapa: "rms_item_code|tag" → valor_tag.
 */
public interface AdItemServicioPort {

    Map<String, String> loadAll();
}

package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.AdItemServicioPort;
import com.omnistack.backend.shared.exception.IntegrationException;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Cache en memoria de AD_ITEM_SERVICIO.
 * Clave: "rms_item_code|tag_lowercase" → valor_tag.
 * Se carga al arrancar junto con el resto de la configuracion.
 */
@Slf4j
@Service
public class AdItemServicioService {

    private final AdItemServicioPort port;
    private Map<String, String> cache = Collections.emptyMap();

    public AdItemServicioService(@Autowired(required = false) AdItemServicioPort port) {
        this.port = port;
    }

    @PostConstruct
    public void init() {
        if (port != null) {
            cache = Collections.unmodifiableMap(port.loadAll());
        } else {
            log.warn("AdItemServicioPort no disponible — configuracion de items no se cargara desde DB");
        }
    }

    /**
     * Retorna el valor del TAG para el rms_item_code dado.
     * Retorna null si no existe el TAG para ese item.
     *
     * Ejemplo: getTag("291843", "EXTERNALOPERATION") → "136"
     *          getTag("291843", "MEDIAID")           → "RETA"
     */
    public String getTag(String rmsItemCode, String tag) {
        if (rmsItemCode == null || tag == null) return null;
        return cache.get(rmsItemCode + "|" + tag.toLowerCase());
    }

    public boolean hasTag(String rmsItemCode, String tag) {
        String value = getTag(rmsItemCode, tag);
        return value != null && !value.isBlank();
    }

    public String requireTag(String rmsItemCode, String tag, String providerName) {
        String value = getTag(rmsItemCode, tag);
        if (value == null || value.isBlank()) {
            throw new IntegrationException(
                    providerName + " no tiene " + tag + " configurado en AD_ITEM_SERVICIO para rms_item_code=" + rmsItemCode);
        }
        return value;
    }
}

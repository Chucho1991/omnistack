package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.ComprobanteStoragePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.shared.constants.ApiPaths;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Decodifica el comprobante en base64 devuelto por los proveedores (Tradicionales/Pega3),
 * lo guarda via {@link ComprobanteStoragePort} y arma la URL publica para servirlo,
 * en vez de exponer el base64 directamente al front.
 */
@Service
@RequiredArgsConstructor
public class ComprobanteUrlService {

    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final ComprobanteStoragePort comprobanteStoragePort;
    private final AppProperties appProperties;

    /**
     * @return la URL del comprobante almacenado, o null si no habia contenido que guardar.
     */
    public String storeAndBuildUrl(String base64Content) {
        if (base64Content == null || base64Content.isBlank()) {
            return null;
        }
        byte[] content;
        try {
            content = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new IntegrationException("El comprobante recibido del proveedor no es base64 valido");
        }
        String id = comprobanteStoragePort.store(content, CONTENT_TYPE_PDF);
        String baseUrl = appProperties.getComprobantes().getPublicBaseUrl();
        return baseUrl + ApiPaths.V1_COMPROBANTES + "/" + id;
    }
}

package com.omnistack.backend.application.port.out;

import java.util.Optional;

/**
 * Almacena comprobantes (PDF) decodificados de los proveedores y permite recuperarlos
 * por id para servirlos via URL en vez de exponer el base64 directamente al front.
 */
public interface ComprobanteStoragePort {

    /**
     * Guarda el contenido y retorna el id con el que se recupera despues (incluye extension).
     */
    String store(byte[] content, String contentType);

    /**
     * Recupera un comprobante previamente guardado por su id.
     */
    Optional<StoredComprobante> load(String id);

    record StoredComprobante(byte[] content, String contentType) {
    }
}

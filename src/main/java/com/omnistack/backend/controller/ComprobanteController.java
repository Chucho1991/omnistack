package com.omnistack.backend.controller;

import com.omnistack.backend.application.port.out.ComprobanteStoragePort;
import com.omnistack.backend.shared.constants.ApiPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sirve los comprobantes de venta (PDF) generados en VERIFY, guardados por
 * {@link com.omnistack.backend.application.service.ComprobanteUrlService}.
 */
@RestController
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteStoragePort comprobanteStoragePort;

    @GetMapping(ApiPaths.V1_COMPROBANTES + "/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        return comprobanteStoragePort.load(id)
                .map(stored -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(stored.contentType()))
                        .body(stored.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

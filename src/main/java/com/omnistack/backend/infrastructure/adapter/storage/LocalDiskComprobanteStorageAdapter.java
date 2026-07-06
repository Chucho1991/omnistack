package com.omnistack.backend.infrastructure.adapter.storage;

import com.omnistack.backend.application.port.out.ComprobanteStoragePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Guarda los comprobantes en disco local, bajo {@code app.comprobantes.storage-path}.
 * La extension del archivo codifica el content-type para poder recuperarlo sin
 * necesitar una tabla/indice de metadata aparte.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalDiskComprobanteStorageAdapter implements ComprobanteStoragePort {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "application/pdf", ".pdf",
            "image/png", ".png",
            "image/jpeg", ".jpg"
    );
    private static final String DEFAULT_EXTENSION = ".bin";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final AppProperties appProperties;

    @Override
    public String store(byte[] content, String contentType) {
        Path dir = Path.of(appProperties.getComprobantes().getStoragePath());
        String extension = EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, DEFAULT_EXTENSION);
        String fileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), content);
        } catch (IOException e) {
            throw new IntegrationException("No se pudo almacenar el comprobante en disco: " + e.getMessage(), e);
        }
        return fileName;
    }

    @Override
    public Optional<StoredComprobante> load(String id) {
        Path dir = Path.of(appProperties.getComprobantes().getStoragePath());
        Path target = dir.resolve(id).normalize();
        if (!target.startsWith(dir.normalize()) || !Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            byte[] content = Files.readAllBytes(target);
            return Optional.of(new StoredComprobante(content, contentTypeFor(id)));
        } catch (IOException e) {
            log.error("No se pudo leer comprobante {}", id, e);
            return Optional.empty();
        }
    }

    private String contentTypeFor(String id) {
        return EXTENSION_BY_CONTENT_TYPE.entrySet().stream()
                .filter(entry -> id.endsWith(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(DEFAULT_CONTENT_TYPE);
    }
}

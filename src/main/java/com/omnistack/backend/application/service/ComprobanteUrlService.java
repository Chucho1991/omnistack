package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.ComprobanteStoragePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.shared.constants.ApiPaths;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

/**
 * Decodifica el comprobante en base64 devuelto por los proveedores (Tradicionales/Pega3),
 * lo guarda via {@link ComprobanteStoragePort} y arma la URL publica para servirlo,
 * en vez de exponer el base64 directamente al front. El proveedor documenta el comprobante
 * como PDF, pero el front necesita consumirlo siempre como PNG — se renderiza la primera
 * pagina del PDF a imagen antes de guardar.
 */
@Service
@RequiredArgsConstructor
public class ComprobanteUrlService {

    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final String CONTENT_TYPE_PNG = "image/png";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final ComprobanteStoragePort comprobanteStoragePort;
    private final AppProperties appProperties;

    /**
     * @param base64Content contenido del comprobante en base64.
     * @param providerContentType content_type declarado por el proveedor en su propia respuesta
     *                             (ej. "application/pdf" en GenerarComprobanteVenta/Pega) — fuente
     *                             autoritativa; si viene null/vacio se hace fallback a sniffing por bytes.
     * @return la URL del comprobante almacenado, o null si no habia contenido que guardar.
     */
    public String storeAndBuildUrl(String base64Content, String providerContentType) {
        if (base64Content == null || base64Content.isBlank()) {
            return null;
        }
        byte[] content;
        try {
            content = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new IntegrationException("El comprobante recibido del proveedor no es base64 valido");
        }
        String contentType = providerContentType != null && !providerContentType.isBlank()
                ? providerContentType
                : detectContentType(content);
        if (CONTENT_TYPE_PDF.equalsIgnoreCase(contentType)) {
            content = renderFirstPageAsPng(content);
            contentType = CONTENT_TYPE_PNG;
        }
        String id = comprobanteStoragePort.store(content, contentType);
        String baseUrl = appProperties.getComprobantes().getPublicBaseUrl();
        return baseUrl + ApiPaths.V1_COMPROBANTES + "/" + id;
    }

    private byte[] renderFirstPageAsPng(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150, ImageType.RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IntegrationException("No se pudo convertir el comprobante PDF a PNG: " + e.getMessage(), e);
        }
    }

    /**
     * El spec de Loteria Nacional documenta el comprobante como PDF, pero no hay garantia de
     * que el proveedor siempre lo entregue asi (ej. podria devolver PNG directamente) — se
     * detecta el tipo real por los primeros bytes del archivo en vez de asumir uno fijo.
     */
    private String detectContentType(byte[] content) {
        if (startsWith(content, PDF_MAGIC)) {
            return CONTENT_TYPE_PDF;
        }
        if (startsWith(content, PNG_MAGIC)) {
            return CONTENT_TYPE_PNG;
        }
        if (startsWith(content, JPEG_MAGIC)) {
            return CONTENT_TYPE_JPEG;
        }
        return CONTENT_TYPE_OCTET_STREAM;
    }

    private boolean startsWith(byte[] content, byte[] magic) {
        if (content.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (content[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}

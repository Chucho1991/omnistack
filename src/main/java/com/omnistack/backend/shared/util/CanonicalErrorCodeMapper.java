package com.omnistack.backend.shared.util;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.shared.constants.ErrorCodes;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Mapea codigos y mensajes tecnicos de proveedores al contrato canonico de errores.
 */
public final class CanonicalErrorCodeMapper {

    private CanonicalErrorCodeMapper() {
    }

    /**
     * Resuelve el codigo canonico de error para una respuesta externa.
     *
     * @param externalResponse respuesta normalizada del proveedor externo
     * @return codigo canonico 00, 01 o 02
     */
    public static String resolve(ExternalTransactionResponse externalResponse) {
        if (externalResponse == null) {
            return ErrorCodes.ERROR_DESCRIPTION_OBTAINED;
        }
        return resolve(externalResponse.getExternalCode(), externalResponse.getExternalMessage());
    }

    /**
     * Resuelve el codigo canonico de error desde codigo y mensaje externos.
     *
     * @param externalCode codigo retornado por el proveedor externo
     * @param externalMessage mensaje retornado por el proveedor externo
     * @return codigo canonico 00, 01 o 02
     */
    public static String resolve(String externalCode, String externalMessage) {
        if (ErrorCodes.OK.equals(externalCode)) {
            return ErrorCodes.OK;
        }
        if (ErrorCodes.INVALID_USER.equals(externalCode) || isInvalidUserMessage(externalMessage)) {
            return ErrorCodes.INVALID_USER;
        }
        return ErrorCodes.ERROR_DESCRIPTION_OBTAINED;
    }

    private static boolean isInvalidUserMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.contains("usuario")
                && (normalized.contains("invalido")
                || normalized.contains("no encontrado")
                || normalized.contains("no existe"));
    }
}

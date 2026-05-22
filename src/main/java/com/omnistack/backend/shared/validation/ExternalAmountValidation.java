package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Valida consistencia entre el monto solicitado y el monto retornado por integraciones externas.
 */
public final class ExternalAmountValidation {

    private ExternalAmountValidation() {
    }

    /**
     * Evalua si el payload externo contiene un monto diferente al monto solicitado.
     *
     * @param request request transaccional recibido por OMNISTACK
     * @param payload payload canonico retornado por el proveedor externo
     * @return resultado de validacion con el monto externo y el mensaje de inconsistencia cuando aplique
     */
    public static Result compare(BaseTransactionRequest request, Map<String, Object> payload) {
        BigDecimal externalAmount = decimalValue(payload, "amount");
        if (request == null
                || request.getAmount() == null
                || externalAmount == null
                || request.getAmount().compareTo(externalAmount) == 0) {
            return new Result(externalAmount, null);
        }

        String direction = request.getAmount().compareTo(externalAmount) > 0 ? "mayor" : "menor";
        return new Result(
                externalAmount,
                "El monto solicitado " + request.getAmount()
                        + " es " + direction
                        + " que el monto retornado por el proveedor externo " + externalAmount);
    }

    /**
     * Obtiene un monto decimal desde un payload canonico.
     *
     * @param payload payload canonico retornado por el proveedor externo
     * @param key nombre del campo que contiene el monto
     * @return monto decimal o null cuando el campo no existe
     * @throws NumberFormatException cuando el valor existe pero no es decimal valido
     */
    public static BigDecimal decimalValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String textValue = String.valueOf(value).trim();
        return textValue.isBlank() ? null : new BigDecimal(textValue);
    }

    /**
     * Resultado de comparacion entre el request interno y el payload externo.
     *
     * @param externalAmount monto retornado por el proveedor externo
     * @param mismatchMessage mensaje funcional cuando los montos no coinciden
     */
    public record Result(BigDecimal externalAmount, String mismatchMessage) {

        /**
         * Indica si existe inconsistencia de monto.
         *
         * @return true cuando el monto externo difiere del monto solicitado
         */
        public boolean hasMismatch() {
            return mismatchMessage != null;
        }
    }
}

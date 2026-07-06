package com.omnistack.backend.application.port.out.strategy;

import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.Map;


public abstract class AbstractProviderStrategy implements TransactionFlowStrategy {

    /**
     * Devuelve la configuracion del proveedor desde DB, o null si no existe.
     */
    protected AppProperties.ProviderProperties findProviderProperties(
            ProviderConfigService configService, String key) {
        return configService.getProviderProperties(key);
    }

    /**
     * Devuelve la configuracion del proveedor desde DB o lanza excepcion si no existe.
     */
    protected AppProperties.ProviderProperties getProviderProperties(
            ProviderConfigService configService, String key, String providerName) {
        AppProperties.ProviderProperties provider = configService.getProviderProperties(key);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor " + providerName);
        }
        return provider;
    }

    /**
     * Indica si existe item en WS_DEFS que coincida con rms_item_code y URL en DB para la operacion.
     * Soporta dos formatos en WS_DEFS:
     *   - Multi-item (nuevo): DEFAULT_CLAVE = "item.{rmsItemCode}" — permite N items por WS_KEY.
     *   - Single-item (legacy): DEFAULT_CLAVE = "item", DEFAULT_VALOR_TEXT = rmsItemCode.
     */
    protected boolean hasConfiguredOperation(
            ProviderWsService wsService,
            ProviderWsDefsService defsService,
            String providerKey,
            Capability capability,
            ServiceDefinition serviceDefinition) {
        String wsKey = toWsKey(capability.name(), serviceDefinition.getMovementType());
        String rmsItemCode = serviceDefinition.getRmsItemCode();
        boolean hasItem = defsService.hasItem(providerKey, wsKey, rmsItemCode)
                || rmsItemCode.equalsIgnoreCase(defsService.getString(providerKey, wsKey, "item"));
        return hasItem && wsService.hasUrl(providerKey, wsKey);
    }

    /**
     * Valida item en WS_DEFS y retorna la URL completa desde DB para la operacion.
     * Soporta los mismos dos formatos que hasConfiguredOperation.
     */
    protected String getRequiredOperationUrl(
            ProviderWsService wsService,
            ProviderWsDefsService defsService,
            String providerKey,
            Capability capability,
            ServiceDefinition serviceDefinition,
            String providerName) {
        String wsKey = toWsKey(capability.name(), serviceDefinition.getMovementType());
        String rmsItemCode = serviceDefinition.getRmsItemCode();
        boolean hasItem = defsService.hasItem(providerKey, wsKey, rmsItemCode)
                || rmsItemCode.equalsIgnoreCase(defsService.getString(providerKey, wsKey, "item"));
        if (!hasItem) {
            throw new IntegrationException(providerName + " no tiene item configurado para rms_item_code="
                    + rmsItemCode + ", capability=" + capability.name()
                    + " y movement_type=" + serviceDefinition.getMovementType());
        }
        return wsService.requireUrl(providerKey, wsKey, providerName);
    }

    /**
     * Construye la clave WS para buscar en IN_OMNI_PROVEEDOR_WS.
     * Formato: CAPABILITY.CASHIN o CAPABILITY.CASHOUT
     *
     * @param capabilityKey clave de la capacidad (ej: "PRECHECK", "PRECHECK_SORTEO")
     * @param movementType  tipo de movimiento
     * @return clave WS (ej: "PRECHECK.CASHIN")
     */
    protected static String toWsKey(String capabilityKey, MovementType movementType) {
        return capabilityKey + "." + (movementType == MovementType.CASH_IN ? "CASHIN" : "CASHOUT");
    }

    /**
     * Valida que el valor actual coincida con el valor esperado de la configuracion del proveedor.
     *
     * @param fieldName nombre del campo para el mensaje de error
     * @param currentValue valor recibido en el request
     * @param expectedValue valor esperado segun la configuracion
     * @param providerName nombre legible del proveedor para el mensaje de error
     * @throws IntegrationException si el valor no coincide o la configuracion esta incompleta
     */
    protected void validateValue(
            String fieldName,
            String currentValue,
            String expectedValue,
            String providerName) {
        if (expectedValue == null || expectedValue.isBlank()) {
            throw new IntegrationException(
                    "La configuracion de " + providerName + " no define el valor requerido para " + fieldName);
        }
        if (!expectedValue.equalsIgnoreCase(currentValue)) {
            throw new IntegrationException(
                    "La solicitud no coincide con la configuracion esperada de " + providerName + " para " + fieldName);
        }
    }

    /**
     * Resuelve un valor por rms_item_code desde IN_OMNI_PROVEEDOR_WS_DEFS cuando el request no
     * lo trae explicito. Usa el formato multi-item "{fieldPrefix}.{rmsItemCode}" (mismo patron que
     * "item.{rmsItemCode}"). Lanza excepcion si no hay valor explicito NI configurado en DB — evita
     * caer en un default fijo en codigo que puede no aplicar al item real que se esta consultando.
     *
     * @param explicitValue valor recibido en el request, si vino
     * @param defsService servicio de WS_DEFS
     * @param providerKey clave del proveedor
     * @param wsKey clave de operacion (ej: "PRECHECK.CASHIN")
     * @param fieldPrefix prefijo del campo en WS_DEFS (ej: "juego_id")
     * @param rmsItemCode item consultado
     * @param providerName nombre legible del proveedor para el mensaje de error
     * @throws IntegrationException si no hay valor explicito ni configurado
     */
    protected String resolveItemDefault(
            String explicitValue,
            ProviderWsDefsService defsService,
            String providerKey,
            String wsKey,
            String fieldPrefix,
            String rmsItemCode,
            String providerName) {
        if (explicitValue != null && !explicitValue.isBlank()) {
            return explicitValue;
        }
        String derived = defsService.getString(providerKey, wsKey, fieldPrefix + "." + rmsItemCode);
        if (derived == null || derived.isBlank()) {
            throw new IntegrationException(providerName + ": falta '" + fieldPrefix
                    + "' en el request y no hay default configurado en IN_OMNI_PROVEEDOR_WS_DEFS para '"
                    + fieldPrefix + "." + rmsItemCode + "' (wsKey=" + wsKey + ")");
        }
        return derived;
    }

    /**
     * Extrae un valor del payload como String.
     *
     * @param payload mapa de datos del proveedor
     * @param key clave del campo
     * @return valor como String, o null si no existe
     */
    protected String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Extrae un valor del payload o retorna un fallback si es null o vacio.
     *
     * @param payload mapa de datos del proveedor
     * @param key clave del campo
     * @param fallback valor de respaldo
     * @return valor encontrado o fallback
     */
    protected String resolveValue(Map<String, Object> payload, String key, String fallback) {
        String value = stringValue(payload, key);
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Extrae un valor del payload como Integer.
     *
     * @param payload mapa de datos del proveedor
     * @param key clave del campo
     * @return valor como Integer, o null si no existe
     */
    protected Integer integerValue(Map<String, Object> payload, String key) {
        String value = stringValue(payload, key);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    /**
     * Extrae un valor del payload como BigDecimal.
     *
     * @param payload mapa de datos del proveedor
     * @param key clave del campo
     * @return valor como BigDecimal, o null si no existe
     */
    protected BigDecimal decimalValue(Map<String, Object> payload, String key) {
        String value = stringValue(payload, key);
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

}

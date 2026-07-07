package com.omnistack.backend.application.service;

import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de validacion de montos por transaccion individual.
 *
 * <p>Valida que el monto de una transaccion no exceda el MONTO_MAX ni sea inferior
 * al MONTO_MIN configurado en la tabla AD_SERVICIO_PARAMETROS para el item.
 *
 * <p>Este control es por transaccion y no involucra logica de cupos diarios
 * (a diferencia del control de CASH_OUT).
 */
@Slf4j
@Service
public class TransactionAmountValidationService {

    /**
     * Valida que el monto de la transaccion este dentro del rango permitido
     * [MONTO_MIN, MONTO_MAX] configurado para el item en AD_SERVICIO_PARAMETROS.
     *
     * @param amount            monto de la transaccion
     * @param serviceDefinition definicion del servicio con los limites configurados
     * @throws BusinessException si el monto excede el maximo o es inferior al minimo
     */
    public void validate(BigDecimal amount, ServiceDefinition serviceDefinition) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal maxAmount = serviceDefinition.getMaxAmount();
        BigDecimal minAmount = serviceDefinition.getMinAmount();

        // Validacion de monto maximo
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            log.warn("Monto transaccion excede MONTO_MAX: monto={}, maxPermitido={}, item={}, proveedor={}",
                    amount, maxAmount, serviceDefinition.getRmsItemCode(),
                    serviceDefinition.getServiceProviderCode());
            throw new BusinessException(
                    "El monto de la transaccion ($" + amount.toPlainString()
                            + ") excede el maximo permitido ($" + maxAmount.toPlainString()
                            + ") para el item " + serviceDefinition.getRmsItemCode());
        }

        // Validacion de monto minimo
        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            log.warn("Monto transaccion inferior a MONTO_MIN: monto={}, minPermitido={}, item={}, proveedor={}",
                    amount, minAmount, serviceDefinition.getRmsItemCode(),
                    serviceDefinition.getServiceProviderCode());
            throw new BusinessException(
                    "El monto de la transaccion ($" + amount.toPlainString()
                            + ") es inferior al minimo permitido ($" + minAmount.toPlainString()
                            + ") para el item " + serviceDefinition.getRmsItemCode());
        }

        log.debug("Validacion monto OK: monto={}, rango=[{}, {}], item={}",
                amount, minAmount, maxAmount, serviceDefinition.getRmsItemCode());
    }
}

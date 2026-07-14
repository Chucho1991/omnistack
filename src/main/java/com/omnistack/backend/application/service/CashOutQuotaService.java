package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.CashOutQuotaPort;
import com.omnistack.backend.domain.enums.CashOutQuotaStatus;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.CashOutQuotaEntry;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio de dominio para el control de cupos diarios de CASH_OUT por local.
 *
 * <p>Reglas de negocio:
 * <ul>
 *   <li>El MONTO_MAX del catalogo es el cupo maximo diario por local para un item CASH_OUT.</li>
 *   <li>Tambien es el monto maximo por transaccion individual.</li>
 *   <li>En el precheck se registra una RESERVA que descuenta cupo disponible.</li>
 *   <li>En el execute se CONFIRMA la reserva.</li>
 *   <li>Si no se confirma en N minutos, un scheduler la EXPIRA y restituye el cupo.</li>
 *   <li>Un reverse en el mismo dia REVIERTE la reserva y restituye el cupo.</li>
 *   <li>Un reverse en fecha posterior NO restituye cupo (pertenece a otro dia).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashOutQuotaService {

    private final CashOutQuotaPort cashOutQuotaPort;

    @Value("${app.cashout-quota.reservation-timeout-minutes:30}")
    private int reservationTimeoutMinutes;

    /**
     * Determina si el servicio aplica para control de cupo CASH_OUT.
     *
     * @param serviceDefinition definicion del servicio del catalogo
     * @return true si el movimiento es CASH_OUT
     */
    public boolean appliesTo(ServiceDefinition serviceDefinition) {
        return serviceDefinition.getMovementType() == MovementType.CASH_OUT;
    }

    /**
     * Reserva cupo en el precheck. Valida que el monto no exceda el maximo por transaccion
     * ni el cupo diario disponible del local. Si es valido, registra la reserva.
     *
     * @param uuid                UUID de la transaccion
     * @param chain               codigo de cadena
     * @param store               codigo de farmacia/local
     * @param pos                 identificador del POS
     * @param rmsItemCode         codigo del item RMS
     * @param serviceProviderCode codigo del proveedor de servicio
     * @param amount              monto de la transaccion
     * @param maxAmount           monto maximo (del catalogo AD_SERVICIO_PARAMETROS)
     * @throws BusinessException si el monto excede el maximo por transaccion o el cupo diario
     */
    public void reserveQuota(String uuid, String chain, String store, String pos,
                             String rmsItemCode, String serviceProviderCode,
                             BigDecimal amount, BigDecimal maxAmount) {
        LocalDate today = LocalDate.now();

        // Idempotencia: si ya existe un registro con este UUID, no insertar y dejar que
        // el flujo continue normalmente para devolver la respuesta al POS
        var existing = cashOutQuotaPort.findByUuid(uuid);
        if (existing.isPresent()) {
            log.info("Reserva de cupo CASH_OUT ya existe, se omite insert (idempotencia). uuid={}, estado={}",
                    uuid, existing.get().getStatus());
            return;
        }

        // Validacion 1: monto no excede el maximo por transaccion
        if (amount.compareTo(maxAmount) > 0) {
            throw new BusinessException(
                    "El monto de la transaccion ($" + amount.toPlainString()
                            + ") excede el maximo permitido por transaccion ($" + maxAmount.toPlainString() + ")");
        }

        // Validacion 2: cupo diario disponible
        BigDecimal consumed = cashOutQuotaPort.getConsumedQuota(chain, store, rmsItemCode, today);
        BigDecimal available = maxAmount.subtract(consumed);

        log.debug("Control cupo CASH_OUT: store={}, item={}, maxDiario={}, consumido={}, disponible={}, solicitado={}",
                store, rmsItemCode, maxAmount, consumed, available, amount);

        if (amount.compareTo(available) > 0) {
            throw new BusinessException(
                    "Cupo diario CASH_OUT insuficiente para el local. "
                            + "Disponible: $" + available.toPlainString()
                            + ", Solicitado: $" + amount.toPlainString()
                            + ", Maximo diario: $" + maxAmount.toPlainString());
        }

        // Registrar reserva
        CashOutQuotaEntry entry = CashOutQuotaEntry.builder()
                .uuid(uuid)
                .chain(chain)
                .store(store)
                .pos(pos)
                .rmsItemCode(rmsItemCode)
                .serviceProviderCode(serviceProviderCode)
                .amount(amount)
                .status(CashOutQuotaStatus.RESERVADO)
                .operationDate(today)
                .build();

        cashOutQuotaPort.saveReservation(entry);
        log.info("Reserva de cupo CASH_OUT registrada. uuid={}, store={}, monto={}, cupoRestante={}",
                uuid, store, amount, available.subtract(amount));
    }

    /**
     * Confirma la reserva de cupo al ejecutarse exitosamente el execute.
     *
     * @param uuid UUID de la transaccion
     */
    public void confirmQuota(String uuid) {
        boolean confirmed = cashOutQuotaPort.confirmReservation(uuid);
        if (confirmed) {
            log.info("Cupo CASH_OUT confirmado (execute exitoso). uuid={}", uuid);
        } else {
            log.warn("No se encontro reserva pendiente para confirmar cupo CASH_OUT. uuid={}", uuid);
        }
    }

    /**
     * Restituye el cupo al ejecutarse un reverse exitoso.
     * Solo restituye si el reverse se realiza en el mismo dia de la transaccion original.
     *
     * @param uuid UUID de la transaccion a revertir
     */
    public void revertQuota(String uuid) {
        LocalDate today = LocalDate.now();
        boolean reverted = cashOutQuotaPort.revertReservation(uuid, today);
        if (reverted) {
            log.info("Cupo CASH_OUT restituido por reverso (mismo dia). uuid={}", uuid);
        } else {
            log.info("Cupo CASH_OUT NO restituido por reverso (fecha posterior o estado no aplica). uuid={}", uuid);
        }
    }

    /**
     * Expira reservas que no fueron confirmadas dentro del timeout configurado.
     * Invocado por el scheduler periodico.
     *
     * @return cantidad de reservas expiradas
     */
    public int expireStaleReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(reservationTimeoutMinutes);
        int expired = cashOutQuotaPort.expireStaleReservations(cutoff);
        if (expired > 0) {
            log.info("Scheduler: {} reservas de cupo CASH_OUT expiradas (timeout={}min, cutoff={})",
                    expired, reservationTimeoutMinutes, cutoff);
        }
        return expired;
    }
}

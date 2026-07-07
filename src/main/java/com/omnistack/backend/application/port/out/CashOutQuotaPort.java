package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.CashOutQuotaEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Puerto de salida para la gestion de cupos diarios de CASH_OUT por local.
 */
public interface CashOutQuotaPort {

    /**
     * Obtiene la suma de montos activos (RESERVADO + CONFIRMADO) para un local/item en una fecha dada.
     *
     * @param chain         codigo de cadena
     * @param store         codigo de farmacia/local
     * @param rmsItemCode   codigo del item RMS
     * @param operationDate fecha de operacion
     * @return suma de montos consumidos en el dia (reservados + confirmados)
     */
    BigDecimal getConsumedQuota(String chain, String store, String rmsItemCode, LocalDate operationDate);

    /**
     * Registra una reserva de cupo (estado RESERVADO) generada en el precheck.
     *
     * @param entry datos de la reserva
     */
    void saveReservation(CashOutQuotaEntry entry);

    /**
     * Confirma una reserva existente (cambia estado de RESERVADO a CONFIRMADO).
     *
     * @param uuid UUID de la transaccion cuya reserva se confirma
     * @return true si se actualizo exitosamente, false si no se encontro reserva pendiente
     */
    boolean confirmReservation(String uuid);

    /**
     * Revierte una reserva/confirmacion existente del mismo dia (cambia estado a REVERTIDO).
     *
     * @param uuid          UUID de la transaccion a revertir
     * @param operationDate fecha de operacion del reverso (para validar que sea el mismo dia)
     * @return true si se actualizo exitosamente, false si no aplica restitucion
     */
    boolean revertReservation(String uuid, LocalDate operationDate);

    /**
     * Expira todas las reservas en estado RESERVADO cuya fecha de reserva excede el timeout.
     *
     * @param cutoffTimestamp timestamp de corte; reservas anteriores a este momento se expiran
     * @return cantidad de registros expirados
     */
    int expireStaleReservations(LocalDateTime cutoffTimestamp);

    /**
     * Busca una entrada de cupo por UUID.
     *
     * @param uuid UUID de la transaccion
     * @return entrada encontrada o vacio
     */
    Optional<CashOutQuotaEntry> findByUuid(String uuid);
}

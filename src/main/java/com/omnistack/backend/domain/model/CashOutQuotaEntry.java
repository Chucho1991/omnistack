package com.omnistack.backend.domain.model;

import com.omnistack.backend.domain.enums.CashOutQuotaStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * Registro de bitacora para control de cupo diario de CASH_OUT por local.
 *
 * <p>Cada entrada representa una reserva de cupo generada en el precheck,
 * que luego puede confirmarse (execute), expirar (timeout) o revertirse (reverse).
 */
@Value
@Builder(toBuilder = true)
public class CashOutQuotaEntry {

    Long id;
    String uuid;
    String chain;
    String store;
    String pos;
    String rmsItemCode;
    String serviceProviderCode;
    BigDecimal amount;
    CashOutQuotaStatus status;
    LocalDate operationDate;
    LocalDateTime reservationTimestamp;
    LocalDateTime confirmationTimestamp;
    LocalDateTime expirationTimestamp;
    LocalDateTime reversalTimestamp;
}

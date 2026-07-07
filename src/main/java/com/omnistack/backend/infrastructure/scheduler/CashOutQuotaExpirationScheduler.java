package com.omnistack.backend.infrastructure.scheduler;

import com.omnistack.backend.application.service.CashOutQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que expira reservas de cupo CASH_OUT no confirmadas dentro del timeout configurado.
 *
 * <p>Ejecuta periodicamente segun {@code app.cashout-quota.expiration-scheduler-rate-ms}
 * y expira todas las reservas en estado RESERVADO cuya antiguedad excede
 * {@code app.cashout-quota.reservation-timeout-minutes}.
 * Las reservas expiradas restituyen su monto al cupo disponible del local.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashOutQuotaExpirationScheduler {

    private final CashOutQuotaService cashOutQuotaService;

    /**
     * Ejecuta la expiracion de reservas CASH_OUT pendientes.
     */
    @Scheduled(fixedDelayString = "${app.cashout-quota.expiration-scheduler-rate-ms:60000}",
               initialDelayString = "${app.cashout-quota.expiration-scheduler-rate-ms:60000}")
    public void expireStaleReservations() {
        try {
            int expired = cashOutQuotaService.expireStaleReservations();
            if (expired > 0) {
                log.info("CashOutQuotaExpirationScheduler: {} reservas expiradas en este ciclo", expired);
            }
        } catch (Exception ex) {
            log.error("Error en scheduler de expiracion de cupos CASH_OUT: {}", ex.getMessage(), ex);
        }
    }
}

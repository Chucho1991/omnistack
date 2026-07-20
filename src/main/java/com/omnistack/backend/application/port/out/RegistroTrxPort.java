package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.RegistroTrx;
import java.util.Optional;

/**
 * Puerto de salida para el registro de transacciones completadas.
 */
public interface RegistroTrxPort {

    /**
     * Persiste un registro de transaccion.
     *
     * @param entry datos de la transaccion a registrar
     */
    void save(RegistroTrx entry);

    /**
     * Busca el codigo de autorizacion original del proveedor (AUTHORIZATION)
     * a partir del codigo homologado almacenado en CP_VAR1.
     *
     * @param homologatedCode codigo homologado generado por OmniStack
     * @return el AUTHORIZATION original del proveedor, o vacio si no se encuentra
     */
    Optional<String> findOriginalAuthByHomologatedCode(String homologatedCode);

    /**
     * Busca el uuid que se uso en el EXECUTE original (donde Pega3 crea y vende el ticket
     * via CrearTicket) a partir del ticketNumber (AUTHORIZATION) devuelto por el proveedor —
     * solo Pega3, para reusarlo como "transaccion" en GenerarComprobantePega (VERIFY) sin
     * depender de que el POS lo reenvie.
     *
     * @param authorization ticketNumber (gameTicketNumber) del ticket ya vendido
     * @return el uuid usado en el EXECUTE original, o vacio si no se encuentra
     */
    Optional<String> findExecuteUuidByAuthorization(String authorization);
}

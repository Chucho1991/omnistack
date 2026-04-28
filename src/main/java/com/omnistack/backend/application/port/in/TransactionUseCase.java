package com.omnistack.backend.application.port.in;

import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.ReverseRequest;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.dto.VerifyRequest;
import com.omnistack.backend.application.dto.VerifyResponse;

/**
 * Puerto de entrada para operaciones transaccionales internas.
 */
public interface TransactionUseCase {

    /**
     * Ejecuta validaciones previas sobre una transaccion.
     *
     * @param request datos de prevalidacion
     * @return resultado de la prevalidacion
     */
    PrecheckResponse precheck(PrecheckRequest request);

    /**
     * Ejecuta una transaccion.
     *
     * @param request datos de ejecucion
     * @return resultado de la ejecucion
     */
    ExecuteResponse execute(ExecuteRequest request);

    /**
     * Verifica el estado de una transaccion.
     *
     * @param request datos de verificacion
     * @return resultado de la verificacion
     */
    VerifyResponse verify(VerifyRequest request);

    /**
     * Reversa una transaccion.
     *
     * @param request datos del reverso
     * @return resultado del reverso
     */
    ReverseResponse reverse(ReverseRequest request);
}

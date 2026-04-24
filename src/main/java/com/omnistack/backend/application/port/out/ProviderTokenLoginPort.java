package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ProviderTokenLoginCommand;
import com.omnistack.backend.domain.model.ProviderTokenLoginResult;

/**
 * Puerto de salida para obtener tokens dinamicos desde proveedores externos.
 */
public interface ProviderTokenLoginPort {

    /**
     * Solicita un nuevo token al proveedor externo.
     *
     * @param command datos requeridos para autenticacion
     * @return token emitido por el proveedor
     */
    ProviderTokenLoginResult login(ProviderTokenLoginCommand command);
}

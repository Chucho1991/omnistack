package com.omnistack.backend.application.port.in;

import com.omnistack.backend.application.dto.ProviderTokenRefreshRequest;
import com.omnistack.backend.application.dto.ProviderTokenRefreshResponse;

/**
 * Puerto de entrada para administracion de tokens de proveedores.
 */
public interface ProviderTokenAdministrationUseCase {

    /**
     * Fuerza la regeneracion del token para el proveedor indicado.
     *
     * @param request proveedor a refrescar
     * @return metadata del token actualizado
     */
    ProviderTokenRefreshResponse refreshToken(ProviderTokenRefreshRequest request);

    /**
     * Refresca los tokens dinamicos configurados para la inicializacion del aplicativo.
     */
    void refreshTokensOnStartup();
}

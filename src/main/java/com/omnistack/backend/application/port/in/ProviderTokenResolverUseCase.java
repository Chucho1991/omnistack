package com.omnistack.backend.application.port.in;

/**
 * Puerto de entrada para resolver el token vigente de un proveedor.
 */
@FunctionalInterface
public interface ProviderTokenResolverUseCase {

    /**
     * Obtiene el token aplicable para el proveedor solicitado.
     *
     * @param categoryCode categoria comercial
     * @param subcategoryCode subcategoria comercial
     * @param serviceProviderCode codigo del proveedor
     * @return token vigente para la integracion
     */
    String getToken(String categoryCode, String subcategoryCode, String serviceProviderCode);
}

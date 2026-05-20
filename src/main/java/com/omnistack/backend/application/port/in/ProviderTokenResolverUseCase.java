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

    /**
     * Fuerza la regeneracion del token aplicable para el proveedor solicitado.
     *
     * @param categoryCode categoria comercial
     * @param subcategoryCode subcategoria comercial
     * @param serviceProviderCode codigo del proveedor
     * @return token regenerado para la integracion
     */
    default String refreshToken(String categoryCode, String subcategoryCode, String serviceProviderCode) {
        return getToken(categoryCode, subcategoryCode, serviceProviderCode);
    }
}

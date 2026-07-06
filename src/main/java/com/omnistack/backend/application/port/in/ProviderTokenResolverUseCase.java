package com.omnistack.backend.application.port.in;

/**
 * Puerto de entrada para resolver el token vigente de un proveedor.
 */
public interface ProviderTokenResolverUseCase {

    /**
     * Obtiene el token aplicable para el proveedor solicitado, resuelto por
     * categoria/subcategoria/codigo de proveedor. OJO: si dos proveedores
     * distintos comparten categoryCode+serviceProviderCode y ninguno tiene
     * subcategoryCode propio configurado (ej. tradicional y pega3, que cada
     * uno cubre varias subcategorias), esta resolucion puede ser ambigua.
     * Preferir {@link #getToken(String)} cuando el caller ya conoce su
     * propia clave de proveedor (todos los WebClientAdapter la tienen como
     * constante PROVIDER_KEY).
     *
     * @param categoryCode categoria comercial
     * @param subcategoryCode subcategoria comercial
     * @param serviceProviderCode codigo del proveedor
     * @return token vigente para la integracion
     */
    String getToken(String categoryCode, String subcategoryCode, String serviceProviderCode);

    /**
     * Obtiene el token aplicable para el proveedor solicitado, resuelto
     * directamente por su clave interna (ej. "tradicional", "pega3") — sin
     * ambiguedad posible, a diferencia de la resolucion por categoria/
     * subcategoria/codigo de proveedor.
     *
     * @param providerKey clave interna del proveedor (PROVEEDOR_KEY en IN_OMNI_PROVEEDOR_CONFIG)
     * @return token vigente para la integracion
     */
    String getToken(String providerKey);

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

    /**
     * Fuerza la regeneracion del token para el proveedor solicitado, resuelto
     * directamente por su clave interna. Ver {@link #getToken(String)}.
     *
     * @param providerKey clave interna del proveedor
     * @return token regenerado para la integracion
     */
    default String refreshToken(String providerKey) {
        return getToken(providerKey);
    }
}

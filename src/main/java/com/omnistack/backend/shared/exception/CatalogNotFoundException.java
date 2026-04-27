package com.omnistack.backend.shared.exception;

/**
 * Excepcion lanzada cuando no existe informacion de catalogo para la consulta.
 */
public class CatalogNotFoundException extends RuntimeException {

    /**
     * Crea una excepcion por ausencia de catalogo.
     *
     * @param message mensaje descriptivo del faltante.
     */
    public CatalogNotFoundException(String message) {
        super(message);
    }
}

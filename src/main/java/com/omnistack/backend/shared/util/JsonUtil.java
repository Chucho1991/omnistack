package com.omnistack.backend.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utilitario para serializacion JSON segura.
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtil() {
    }

    /**
     * Serializa un objeto a JSON y propaga errores como excepcion de estado.
     *
     * @param value objeto a serializar
     * @return representacion JSON
     * @throws IllegalStateException cuando no es posible serializar el objeto
     */
    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar el objeto a JSON", exception);
        }
    }

    /**
     * Serializa un objeto a JSON sin propagar errores.
     *
     * @param value objeto a serializar
     * @return representacion JSON o marcador de error de serializacion
     */
    public static String toJsonSilently(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":true}";
        }
    }
}

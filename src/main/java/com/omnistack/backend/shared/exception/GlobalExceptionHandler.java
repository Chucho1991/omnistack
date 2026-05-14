package com.omnistack.backend.shared.exception;

import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.ErrorResponse;
import com.omnistack.backend.shared.constants.ErrorCodes;
import jakarta.validation.ConstraintViolationException;
import java.text.Normalizer;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo global y uniforme de errores.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validacion de entrada.
     *
     * @param exception excepcion de validacion
     * @return respuesta HTTP de solicitud invalida
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(error(
                ErrorCodes.ERROR_DESCRIPTION_OBTAINED,
                "La solicitud no cumple las validaciones requeridas"));
    }

    /**
     * Maneja errores funcionales de negocio.
     *
     * @param exception excepcion de negocio
     * @return respuesta HTTP de error funcional
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error(
                resolveErrorCode(exception.getMessage()),
                exception.getMessage()));
    }

    /**
     * Maneja errores de integracion externa.
     *
     * @param exception excepcion de integracion
     * @return respuesta HTTP de gateway externo fallido
     */
    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegration(IntegrationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(
                resolveErrorCode(exception.getMessage()),
                exception.getMessage()));
    }

    /**
     * Maneja errores de catalogo no encontrado.
     *
     * @param exception excepcion de catalogo
     * @return respuesta HTTP de recurso no encontrado
     */
    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCatalog(CatalogNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(
                ErrorCodes.ERROR_DESCRIPTION_OBTAINED,
                exception.getMessage()));
    }

    /**
     * Maneja errores no controlados.
     *
     * @param exception excepcion no controlada
     * @return respuesta HTTP de error interno
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        log.error("Unhandled error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(
                ErrorCodes.ERROR_DESCRIPTION_OBTAINED,
                "Ha ocurrido un error interno inesperado"));
    }

    private ErrorResponse error(String errorCode, String message) {
        return ErrorResponse.builder()
                .errorFlag(true)
                .error(ErrorDetail.builder()
                        .code(errorCode)
                        .message(message)
                        .build())
                .build();
    }

    private String resolveErrorCode(String message) {
        if (message == null || message.isBlank()) {
            return ErrorCodes.ERROR_DESCRIPTION_OBTAINED;
        }
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.contains("usuario invalido")
                ? ErrorCodes.INVALID_USER
                : ErrorCodes.ERROR_DESCRIPTION_OBTAINED;
    }
}

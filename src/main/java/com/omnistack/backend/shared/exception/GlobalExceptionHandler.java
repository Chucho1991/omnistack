package com.omnistack.backend.shared.exception;

import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.shared.constants.ErrorCodes;
import jakarta.validation.ConstraintViolationException;
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

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorDetail> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(error(
                ErrorCodes.VALIDATION_ERROR,
                "La solicitud no cumple las validaciones requeridas"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorDetail> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error(
                ErrorCodes.BUSINESS_ERROR,
                exception.getMessage()));
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorDetail> handleIntegration(IntegrationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(
                ErrorCodes.INTEGRATION_ERROR,
                exception.getMessage()));
    }

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ErrorDetail> handleCatalog(CatalogNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(
                ErrorCodes.CATALOG_NOT_FOUND,
                exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetail> handleGeneric(Exception exception) {
        log.error("Unhandled error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(
                ErrorCodes.INTERNAL_ERROR,
                "Ha ocurrido un error interno inesperado"));
    }

    private ErrorDetail error(String errorCode, String message) {
        return ErrorDetail.builder()
                .code(errorCode)
                .message(message)
                .build();
    }
}

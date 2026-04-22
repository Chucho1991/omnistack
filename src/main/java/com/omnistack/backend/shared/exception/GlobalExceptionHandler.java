package com.omnistack.backend.shared.exception;

import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.shared.constants.ErrorCodes;
import com.omnistack.backend.shared.constants.StatusCodes;
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
    public ResponseEntity<ApiErrorResponse> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(error(
                ErrorCodes.VALIDATION_ERROR,
                "La solicitud no cumple las validaciones requeridas",
                StatusCodes.VALIDATION_FAILED,
                "VALIDATION_ERROR"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error(
                ErrorCodes.BUSINESS_ERROR,
                exception.getMessage(),
                StatusCodes.BUSINESS_REJECTED,
                "BUSINESS_ERROR"));
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegration(IntegrationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(
                ErrorCodes.INTEGRATION_ERROR,
                exception.getMessage(),
                StatusCodes.INTEGRATION_FAILED,
                "INTEGRATION_ERROR"));
    }

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCatalog(CatalogNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(
                ErrorCodes.CATALOG_NOT_FOUND,
                exception.getMessage(),
                StatusCodes.CATALOG_UNAVAILABLE,
                "CATALOG_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception) {
        log.error("Unhandled error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(
                ErrorCodes.INTERNAL_ERROR,
                "Ha ocurrido un error interno inesperado",
                StatusCodes.INTEGRATION_FAILED,
                "INTERNAL_ERROR"));
    }

    private ApiErrorResponse error(String errorCode, String message, String statusCode, String statusMessage) {
        return ApiErrorResponse.builder()
                .isError(true)
                .error(ErrorDetail.builder().code(errorCode).message(message).build())
                .status(StatusDetail.builder().code(statusCode).message(statusMessage).build())
                .build();
    }
}

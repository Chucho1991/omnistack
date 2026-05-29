package com.omnistack.backend.infrastructure.adapter.integration.ecuabet;

import org.springframework.web.reactive.function.client.WebClientRequestException;

final class EcuabetTransportErrorMapper {

    private EcuabetTransportErrorMapper() {
    }

    static String buildMessage(String operationName, String url, WebClientRequestException exception) {
        String rootCause = resolveRootCause(exception);
        if (hasCause(exception, "SSLHandshakeException")
                || hasCause(exception, "StacklessSSLHandshakeException")
                || hasCause(exception, "ClosedChannelException")
                || hasCause(exception, "NativeIoException")) {
            return "Error SSL/TLS al invocar ECUABET " + operationName
                    + ". Revise protocolo TLS, SNI, truststore y conectividad hacia " + url
                    + ". Causa: " + rootCause;
        }
        return "Error de transporte al invocar ECUABET " + operationName + " en " + url
                + ". Causa: " + rootCause;
    }

    private static String resolveRootCause(Throwable throwable) {
        Throwable cursor = throwable;
        Throwable rootCause = throwable;
        while (cursor != null) {
            rootCause = cursor;
            cursor = cursor.getCause();
        }
        String message = rootCause.getMessage();
        return message != null && !message.isBlank()
                ? rootCause.getClass().getSimpleName() + ": " + message
                : rootCause.getClass().getSimpleName();
    }

    private static boolean hasCause(Throwable throwable, String simpleClassName) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor.getClass().getSimpleName().equals(simpleClassName)) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}

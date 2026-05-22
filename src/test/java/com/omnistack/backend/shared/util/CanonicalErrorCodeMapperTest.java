package com.omnistack.backend.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import org.junit.jupiter.api.Test;

class CanonicalErrorCodeMapperTest {

    @Test
    void shouldKeepCanonicalSuccessCode() {
        assertEquals("00", CanonicalErrorCodeMapper.resolve("00", ""));
    }

    @Test
    void shouldMapUnknownExternalCodesToGenericError() {
        ExternalTransactionResponse externalResponse = ExternalTransactionResponse.builder()
                .externalCode("400066")
                .externalMessage("Tiempo excedido para realizar un reverso de recarga")
                .build();

        assertEquals("01", CanonicalErrorCodeMapper.resolve(externalResponse));
    }

    @Test
    void shouldMapInvalidUserMessagesToInvalidUserCode() {
        assertEquals("02", CanonicalErrorCodeMapper.resolve("101", "Usuario invalido"));
    }
}

package com.omnistack.backend.shared.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.omnistack.backend.application.dto.CreateTicketRequest;
import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.domain.enums.MovementType;
import org.junit.jupiter.api.Test;

class TransactionRequestValidatorTest {

    private final TransactionRequestValidator validator = new TransactionRequestValidator();

    @Test
    void shouldAllowPrecheckRequestWithoutIdentifier() {
        PrecheckRequest request = PrecheckRequest.builder().build();
        assertTrue(validator.isValid(request, null));
    }

    @Test
    void shouldAllowCreateTicketRequestWithoutIdentifier() {
        CreateTicketRequest request = CreateTicketRequest.builder().drawId("7151").build();
        assertTrue(validator.isValid(request, null));
    }

    @Test
    void shouldRejectExecuteRequestWithoutIdentifierWhenCashIn() {
        ExecuteRequest request = ExecuteRequest.builder().movementType(MovementType.CASH_IN).build();
        assertFalse(validator.isValid(request, null));
    }

    @Test
    void shouldAllowExecuteRequestWithDocumentWhenCashIn() {
        ExecuteRequest request = ExecuteRequest.builder().movementType(MovementType.CASH_IN).document("0987654321").build();
        assertTrue(validator.isValid(request, null));
    }
}

package com.omnistack.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.in.TransactionUseCase;
import com.omnistack.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionUseCase transactionUseCase;

    @Test
    void shouldReturnPrecheckResponse() throws Exception {
        when(transactionUseCase.precheck(any())).thenReturn(PrecheckResponse.builder()
                .uuid("uuid-1")
                .errorFlag(false)
                .status(new StatusDetail("00", "PRECHECK completado correctamente"))
                .build());

        mockMvc.perform(post("/v1/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uuid":"uuid-1",
                                  "chain":"001",
                                  "store":"0001",
                                  "pos":"POS-01",
                                  "channelPos":"POS",
                                  "movementType":"CASH_IN",
                                  "categoryCode":"REC",
                                  "subcategoryCode":"CEL",
                                  "serviceProviderCode":"CLARO",
                                  "rmsItemCode":"900001",
                                  "amount":10.00,
                                  "phone":"0999999999"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("uuid-1"))
                .andExpect(jsonPath("$.status.code").value("00"));
    }
}

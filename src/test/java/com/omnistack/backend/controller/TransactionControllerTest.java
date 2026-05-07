package com.omnistack.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.dto.VerifyResponse;
import com.omnistack.backend.application.port.in.TransactionUseCase;
import com.omnistack.backend.shared.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
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

    @Test
    void shouldAcceptCashoutPrecheckRequest() throws Exception {
        when(transactionUseCase.precheck(any())).thenReturn(PrecheckResponse.builder()
                .uuid("uuid-cashout")
                .errorFlag(false)
                .status(new StatusDetail("00", "Transaccion correcta"))
                .build());

        mockMvc.perform(post("/v1/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uuid":"uuid-cashout",
                                  "chain":"1",
                                  "store":"148",
                                  "store_name":"FYBECA AMAZONAS",
                                  "pos":"1",
                                  "channel_POS":"POS",
                                  "category_code":"1",
                                  "subcategory_code":"1",
                                  "service_provider_code":"1",
                                  "rms_item_code":"100708846",
                                  "userid":"",
                                  "phone":"",
                                  "withdrawId":"7667",
                                  "password":"88422",
                                  "document":"",
                                  "amount":1.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("uuid-cashout"))
                .andExpect(jsonPath("$.status.code").value("00"));
    }

    @Test
    void shouldAcceptEcuabetCashoutExecuteRequest() throws Exception {
        when(transactionUseCase.execute(any())).thenReturn(ExecuteResponse.builder()
                .uuid("uuid-cashout-execute")
                .errorFlag(false)
                .authorization("10980")
                .status(new StatusDetail("0", "Transacci\u00F3n correcta"))
                .build());

        mockMvc.perform(post("/v1/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uuid":"uuid-cashout-execute",
                                  "chain":"1",
                                  "store":"148",
                                  "store_name":"FYBECA EL BATAN",
                                  "pos":"1",
                                  "channel_POS":"POS",
                                  "category_code":"1",
                                  "subcategory_code":"1",
                                  "service_provider_code":"1",
                                  "rms_item_code":"100708846",
                                  "withdrawId":"7668",
                                  "password":"77992",
                                  "document":"0912345678",
                                  "amount":25.50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("uuid-cashout-execute"))
                .andExpect(jsonPath("$.transactionId").doesNotExist())
                .andExpect(jsonPath("$.authorization").value("10980"))
                .andExpect(jsonPath("$.status.code").value("0"));
    }

    @Test
    void shouldAcceptVerifyRequestWithoutAmount() throws Exception {
        when(transactionUseCase.verify(any())).thenReturn(VerifyResponse.builder()
                .uuid("uuid-verify")
                .errorFlag(false)
                .status(new StatusDetail("0", "Transaccion ha sido ejecutada"))
                .build());

        mockMvc.perform(post("/v1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uuid":"uuid-verify",
                                  "chain":"1",
                                  "store":"148",
                                  "store_name":"FYBECA AMAZONAS",
                                  "pos":"1",
                                  "channel_POS":"POS",
                                  "category_code":"1",
                                  "subcategory_code":"1",
                                  "service_provider_code":"2",
                                  "rms_item_code":"100708850",
                                  "authorization":"9F968187-F436-4F19-8C1F-A7A4DA07A899",
                                  "serialnumber":"7366ea56284a06a2a58f561b497386b80fcd3eaea858d0c511",
                                  "document":"0901111112"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("uuid-verify"))
                .andExpect(jsonPath("$.status.code").value("0"));
    }

    @Test
    void shouldAcceptBet593CashoutReverseRequest() throws Exception {
        when(transactionUseCase.reverse(any())).thenReturn(ReverseResponse.builder()
                .uuid("uuid-reverse")
                .errorFlag(false)
                .authorization("ca9b201a-a668-45ed-876c-00affcb18580")
                .document("0901111112")
                .status(new StatusDetail("0", "Transaccion correcta"))
                .build());

        mockMvc.perform(post("/v1/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uuid":"uuid-reverse",
                                  "chain":"1",
                                  "store":"148",
                                  "store_name":"FYBECA AMAZONAS",
                                  "pos":"1",
                                  "channel_POS":"POS",
                                  "category_code":"1",
                                  "subcategory_code":"1",
                                  "service_provider_code":"2",
                                  "rms_item_code":"100708848",
                                  "authorization":"ca9b201a-a668-45ed-876c-00affcb18580",
                                  "document":"0901111112",
                                  "motivo":"Demora en obtener respuesta"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("uuid-reverse"))
                .andExpect(jsonPath("$.authorization").value("ca9b201a-a668-45ed-876c-00affcb18580"))
                .andExpect(jsonPath("$.document").value("0901111112"))
                .andExpect(jsonPath("$.status.code").value("0"));
    }

    @Test
    void shouldAcceptEcuabetCashinReverseRequest() throws Exception {
        when(transactionUseCase.reverse(any())).thenReturn(ReverseResponse.builder()
                .uuid("uuid-ecuabet-reverse")
                .errorFlag(false)
                .authorization("91081")
                .document("0912345678")
                .amount(new BigDecimal("100000.00"))
                .status(new StatusDetail("0", "Transaccion correcta"))
                .build());

        mockMvc.perform(post("/v1/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uuid":"uuid-ecuabet-reverse",
                                  "chain":"1",
                                  "store":"148",
                                  "store_name":"FYBECA EL BATAN",
                                  "pos":"1",
                                  "channel_POS":"POS",
                                  "category_code":"1",
                                  "subcategory_code":"1",
                                  "service_provider_code":"1",
                                  "rms_item_code":"100713841",
                                  "authorization":"91081",
                                  "document":"0912345678",
                                  "amount":100000.00,
                                  "motivo":"Demora en obtener respuesta"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("uuid-ecuabet-reverse"))
                .andExpect(jsonPath("$.authorization").value("91081"))
                .andExpect(jsonPath("$.document").value("0912345678"))
                .andExpect(jsonPath("$.amount").value(100000.00))
                .andExpect(jsonPath("$.status.code").value("0"));
    }
}

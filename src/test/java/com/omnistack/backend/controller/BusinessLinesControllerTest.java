package com.omnistack.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omnistack.backend.application.dto.BusinessLineCollectionSubcategoryResponse;
import com.omnistack.backend.application.dto.BusinessLineInputFieldResponse;
import com.omnistack.backend.application.dto.BusinessLinePaymentMethodResponse;
import com.omnistack.backend.application.dto.BusinessLineProviderResponse;
import com.omnistack.backend.application.dto.BusinessLineServiceResponse;
import com.omnistack.backend.application.dto.BusinessLinesResponse;
import com.omnistack.backend.application.port.in.BusinessLinesUseCase;
import com.omnistack.backend.shared.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BusinessLinesController.class)
@Import(GlobalExceptionHandler.class)
class BusinessLinesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BusinessLinesUseCase businessLinesUseCase;

    @Test
    void shouldReturnBusinessLinesResponseUsingContractFields() throws Exception {
        when(businessLinesUseCase.getBusinessLines(any())).thenReturn(BusinessLinesResponse.builder()
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos("POS")
                .collectionSubcategory(List.of(BusinessLineCollectionSubcategoryResponse.builder()
                        .categoryCode("REC")
                        .categoryName("Recargas")
                        .subcategoryCode("CEL")
                        .subcategoryName("Recargas celulares")
                        .active(true)
                        .serviceProviders(List.of(BusinessLineProviderResponse.builder()
                                .serviceProviderCode("CLARO")
                                .providerName("Claro")
                                .active(true)
                                .services(List.of(BusinessLineServiceResponse.builder()
                                        .rmsItemCode("900001")
                                        .description("Recarga Claro")
                                        .active(true)
                                        .jdeCode("JDE-REC-001")
                                        .movementType("CASH_IN")
                                        .mixedPayment(false)
                                        .flgItem("RECA")
                                        .refund(false)
                                        .minAmount("1.00")
                                        .maxAmount("200.00")
                                        .capabilities(List.of("PRECHECK", "EXECUTE"))
                                        .inputFields(List.of(BusinessLineInputFieldResponse.builder()
                                                .id("phone")
                                                .label("Telefono")
                                                .type("STRING")
                                                .capability("PRECHECK")
                                                .required(true)
                                                .group("PHONE")
                                                .build()))
                                        .paymentMethods(List.of(BusinessLinePaymentMethodResponse.builder()
                                                .servicePaymentMethodId(1)
                                                .paymentMethodCode("EFECTIVO")
                                                .active(true)
                                                .build()))
                                        .requiresConsent(false)
                                        .build()))
                                .build()))
                        .build()))
                .build());

        mockMvc.perform(post("/business-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chain":"1",
                                  "store":"148",
                                  "store_name":"FYBECA AMAZONAS",
                                  "pos":"1",
                                  "channel_POS":"POS",
                                  "movement_type_filter":"CASH_IN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store_name").value("FYBECA AMAZONAS"))
                .andExpect(jsonPath("$.channel_POS").value("POS"))
                .andExpect(jsonPath("$.collection_subcategory[0].is_active").value(true))
                .andExpect(jsonPath("$.collection_subcategory[0].service_providers[0].services[0].payment_methods[0].service_payment_method_id").value(1))
                .andExpect(jsonPath("$.collection_subcategory[0].service_providers[0].services[0].input_fields[0].capability").value("PRECHECK"));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/business-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "store":"148",
                                  "pos":"1",
                                  "channel_POS":"POS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL-001"))
                .andExpect(jsonPath("$.message").value("La solicitud no cumple las validaciones requeridas"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.is_error").doesNotExist());
    }
}

package com.omnistack.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omnistack.backend.application.dto.ProviderTokenRefreshResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.in.ProviderTokenAdministrationUseCase;
import com.omnistack.backend.shared.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProviderTokenController.class)
@Import(GlobalExceptionHandler.class)
class ProviderTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProviderTokenAdministrationUseCase providerTokenAdministrationUseCase;

    @Test
    void shouldRefreshProviderToken() throws Exception {
        when(providerTokenAdministrationUseCase.refreshToken(any())).thenReturn(ProviderTokenRefreshResponse.builder()
                .errorFlag(false)
                .status(StatusDetail.builder().code("00").message("Token actualizado correctamente").build())
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .providerName("LOTERIA NACIONAL")
                .refreshedAt(OffsetDateTime.parse("2026-04-24T11:00:00-05:00"))
                .expiresAt(OffsetDateTime.parse("2026-04-25T11:00:00-05:00"))
                .build());

        mockMvc.perform(post("/v1/provider-token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category_code":"1",
                                  "subcategory_code":"1",
                                  "service_provider_code":"2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category_code").value("1"))
                .andExpect(jsonPath("$.subcategory_code").value("1"))
                .andExpect(jsonPath("$.service_provider_code").value("2"))
                .andExpect(jsonPath("$.status.code").value("00"));
    }

    @Test
    void shouldValidateRefreshRequest() throws Exception {
        mockMvc.perform(post("/v1/provider-token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category_code":"1",
                                  "subcategory_code":"",
                                  "service_provider_code":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL-001"));
    }
}

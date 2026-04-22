package com.omnistack.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omnistack.backend.application.dto.HealthcheckResponse;
import com.omnistack.backend.application.port.in.HealthcheckUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthcheckController.class)
class HealthcheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthcheckUseCase healthcheckUseCase;

    @Test
    void shouldReturnHealthcheckStatus() throws Exception {
        when(healthcheckUseCase.getHealthcheck()).thenReturn(HealthcheckResponse.builder()
                .status("UP")
                .application("omnistack-backend")
                .timestamp("2026-04-22T12:00:00Z")
                .catalogVersion("memory-v1")
                .build());

        mockMvc.perform(get("/healthcheck"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("omnistack-backend"))
                .andExpect(jsonPath("$.catalogVersion").value("memory-v1"));
    }
}

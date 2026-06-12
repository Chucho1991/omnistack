package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradicionalSorteosQueryRequest {
    String userName;
    String token;
    Integer medioId;
    String juegoId;
}

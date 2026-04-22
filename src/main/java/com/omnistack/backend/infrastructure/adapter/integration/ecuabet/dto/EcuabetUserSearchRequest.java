package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para el endpoint ECUABET user/search.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EcuabetUserSearchRequest {
    String shop;
    String token;
    String userid;
    Integer country;
    String phone;
    String document;
}

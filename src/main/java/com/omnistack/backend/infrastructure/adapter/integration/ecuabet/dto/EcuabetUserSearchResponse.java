package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo generico para el endpoint ECUABET user/search.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcuabetUserSearchResponse {
    private String code;
    private Integer error;
    private String name;
    private String userid;
    @JsonProperty("message")
    @JsonAlias("msg")
    private String message;
    private final Map<String, Object> raw = new LinkedHashMap<>();

    @JsonAnySetter
    public void addRawField(String key, Object value) {
        raw.put(key, value);
    }
}

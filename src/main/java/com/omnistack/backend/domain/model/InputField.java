package com.omnistack.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.domain.enums.InputFieldType;
import lombok.Builder;
import lombok.Value;

/**
 * Campo parametrico requerido por un servicio.
 */
@Value
@Builder
public class InputField {
    String id;
    String label;
    @JsonProperty("type")
    InputFieldType type;
    String capability;
    boolean required;
    String group;
    String conditional;
}

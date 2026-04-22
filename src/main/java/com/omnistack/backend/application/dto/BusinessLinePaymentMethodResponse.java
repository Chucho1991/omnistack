package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Metodo de pago permitido para un servicio comercial.
 */
@Value
@Builder
@Schema(description = "Metodo de pago habilitado para el servicio")
public class BusinessLinePaymentMethodResponse {
    @JsonProperty("service_payment_method_id")
    Integer servicePaymentMethodId;
    @JsonProperty("payment_method_code")
    String paymentMethodCode;
    @JsonProperty("is_active")
    boolean active;
}

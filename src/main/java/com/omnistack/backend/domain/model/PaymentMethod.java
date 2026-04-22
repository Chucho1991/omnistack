package com.omnistack.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.domain.enums.PaymentMethodCode;
import lombok.Builder;
import lombok.Value;

/**
 * Metodo de pago permitido para el servicio.
 */
@Value
@Builder
public class PaymentMethod {
    @JsonProperty("service_payment_method_id")
    Integer servicePaymentMethodId;
    @JsonProperty("payment_method_code")
    PaymentMethodCode paymentMethodCode;
    @JsonProperty("is_active")
    boolean active;
    @JsonProperty("description")
    String description;
}

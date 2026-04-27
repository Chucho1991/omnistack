package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Servicio comercial habilitado para un proveedor.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Servicio transaccional disponible para el punto de venta")
public class BusinessLineServiceResponse {
    @JsonProperty("rms_item_code")
    String rmsItemCode;
    String description;
    @JsonProperty("is_active")
    boolean active;
    @JsonProperty("jde_code")
    String jdeCode;
    @JsonProperty("movement_type")
    String movementType;
    @JsonProperty("is_mixed_payment")
    boolean mixedPayment;
    @JsonProperty("flg_item")
    String flgItem;
    @JsonProperty("is_refund")
    boolean refund;
    @JsonProperty("min_amount")
    String minAmount;
    @JsonProperty("max_amount")
    String maxAmount;
    @JsonProperty("timeout_ws_max")
    String timeoutWsMax;
    @JsonProperty("retries_ws_max")
    String retriesWsMax;
    @JsonProperty("num_tickets")
    String numTickets;
    List<String> capabilities;
    @JsonProperty("input_fields")
    List<BusinessLineInputFieldResponse> inputFields;
    @JsonProperty("payment_methods")
    List<BusinessLinePaymentMethodResponse> paymentMethods;
    @JsonProperty("requires_consent")
    boolean requiresConsent;
    @JsonProperty("consent_text")
    String consentText;
}

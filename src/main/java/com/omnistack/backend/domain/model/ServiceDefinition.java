package com.omnistack.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.FlgItem;
import com.omnistack.backend.domain.enums.MovementType;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Definicion interna de un servicio transaccional.
 */
@Value
@Builder
public class ServiceDefinition {
    @JsonProperty("category_code")
    String categoryCode;
    @JsonProperty("subcategory_code")
    String subcategoryCode;
    @JsonProperty("service_provider_code")
    String serviceProviderCode;
    @JsonProperty("rms_item_code")
    String rmsItemCode;
    String description;
    @JsonProperty("is_active")
    boolean active;
    @JsonProperty("jde_code")
    String jdeCode;
    @JsonProperty("movement_type")
    MovementType movementType;
    @JsonProperty("is_mixed_payment")
    boolean mixedPayment;
    @JsonProperty("flg_item")
    FlgItem flgItem;
    @JsonProperty("is_refund")
    boolean refund;
    @JsonProperty("min_amount")
    BigDecimal minAmount;
    @JsonProperty("max_amount")
    BigDecimal maxAmount;
    List<Capability> capabilities;
    @JsonProperty("input_fields")
    List<InputField> inputFields;
    @JsonProperty("payment_methods")
    List<PaymentMethod> paymentMethods;
    @JsonProperty("requires_consent")
    boolean requiresConsent;
    @JsonProperty("consent_text")
    String consentText;
}

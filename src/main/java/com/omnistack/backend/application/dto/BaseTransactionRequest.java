package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Request base compartido por las operaciones transaccionales.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseTransactionRequest {
    @NotBlank
    @Schema(example = "f0908f64-9145-45cf-a22c-c36bca604372")
    private String uuid;

    @NotBlank
    @Size(max = 10)
    @Schema(example = "001")
    private String chain;

    @NotBlank
    @Schema(example = "0001")
    private String store;

    @JsonProperty("store_name")
    @JsonAlias("storeName")
    @Schema(example = "Tienda Centro")
    private String storeName;

    @NotBlank
    @Schema(example = "POS-01")
    private String pos;

    @NotNull
    @JsonProperty("channel_POS")
    @JsonAlias("channelPos")
    @Schema(example = "POS")
    private ChannelPos channelPos;

    @JsonProperty("movement_type")
    @JsonAlias("movementType")
    @Schema(example = "CASH_IN", description = "Campo opcional; OMNISTACK resuelve CASH_IN o CASH_OUT desde el catalogo segun rms_item_code")
    private MovementType movementType;

    @NotBlank
    @JsonProperty("category_code")
    @JsonAlias("categoryCode")
    @Schema(example = "REC")
    private String categoryCode;

    @NotBlank
    @JsonProperty("subcategory_code")
    @JsonAlias("subcategoryCode")
    @Schema(example = "CEL")
    private String subcategoryCode;

    @NotBlank
    @JsonProperty("service_provider_code")
    @JsonAlias("serviceProviderCode")
    @Schema(example = "CLARO")
    private String serviceProviderCode;

    @NotBlank
    @JsonProperty("rms_item_code")
    @JsonAlias("rmsItemCode")
    @Schema(example = "900001")
    private String rmsItemCode;

    @Schema(example = "usr123")
    private String userid;

    @Schema(example = "0999999999")
    private String phone;

    @JsonProperty("withdrawalId")
    @JsonAlias({"withdrawId"})
    @Schema(example = "W123456")
    private String withdrawId;

    @Schema(example = "secure-pass")
    private String password;

    @Schema(example = "0912345678")
    private String document;

    @Schema(example = "AUTH-001")
    private String authorization;

    @Schema(example = "SN001-XYZ")
    private String serialnumber;

    /**
     * Retorna el monto asociado a la solicitud transaccional.
     *
     * @return monto informado en la solicitud
     */
    @Schema(hidden = true)
    public BigDecimal getAmount() {
        return null;
    }
}

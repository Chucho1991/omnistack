package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta diagnostica con toda la parametrizacion asociada a un rms_item_code,
 * clasificada por tabla de origen.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Diagnostico completo de parametrizacion de un item")
public class ItemConfigDiagnosticResponse {

    @JsonProperty("rms_item_code")
    @Schema(example = "100708848")
    private String rmsItemCode;

    @JsonProperty("ad_servicio_parametros")
    @Schema(description = "Configuracion en AD_SERVICIO_PARAMETROS + AD_CANAL_SERVICIO (BD RMS / gpf_omnistack)")
    private AdServicioParametrosSection adServicioParametros;

    @JsonProperty("rms_item_master")
    @Schema(description = "Metadata del item en ITEM_MASTER (BD RMS)")
    private RmsItemMasterSection rmsItemMaster;

    @JsonProperty("rms_item_supplier")
    @Schema(description = "Proveedor del item en ITEM_SUPPLIER + SUPS (BD RMS)")
    private RmsItemSupplierSection rmsItemSupplier;

    @JsonProperty("in_omni_proveedor_config")
    @Schema(description = "Configuracion del proveedor en IN_OMNI_PROVEEDOR_CONFIG (BD TUKUNAFUNC)")
    private List<ProviderConfigEntry> providerConfig;

    @JsonProperty("in_omni_proveedor_ws")
    @Schema(description = "Operaciones WS registradas en IN_OMNI_PROVEEDOR_WS (BD TUKUNAFUNC)")
    private List<ProviderWsEntry> providerWs;

    @JsonProperty("in_omni_proveedor_ws_defs")
    @Schema(description = "Definiciones de items/config en IN_OMNI_PROVEEDOR_WS_DEFS (BD TUKUNAFUNC)")
    private List<ProviderWsDefsEntry> providerWsDefs;

    @JsonProperty("in_omni_input_fields")
    @Schema(description = "Campos de entrada en IN_OMNI_INPUT_FIELDS (BD TUKUNAFUNC)")
    private List<InputFieldEntry> inputFields;

    @JsonProperty("diagnostico")
    @Schema(description = "Resultado del diagnostico automatico: campos faltantes o problemas detectados")
    private List<String> diagnostico;

    // --- Secciones anidadas ---

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class AdServicioParametrosSection {
        @JsonProperty("encontrado")
        private boolean encontrado;
        @JsonProperty("service_provider_code")
        private String serviceProviderCode;
        @JsonProperty("id_config")
        private Long idConfig;
        @JsonProperty("activo_en_canal")
        private Boolean activoEnCanal;
        @JsonProperty("canal_codigo")
        private Integer canalCodigo;
        @JsonProperty("flg_item")
        private String flgItem;
        @JsonProperty("monto_min")
        private String montoMin;
        @JsonProperty("monto_max")
        private String montoMax;
        @JsonProperty("flg_pago_mixto")
        private String flgPagoMixto;
        @JsonProperty("flg_devolucion")
        private String flgDevolucion;
        @JsonProperty("timeout_ws_max")
        private String timeoutWsMax;
        @JsonProperty("retries_ws_max")
        private String retriesWsMax;
        @JsonProperty("num_tickets")
        private String numTickets;
        @JsonProperty("requiere_consentimiento")
        private String requiereConsentimiento;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class RmsItemMasterSection {
        @JsonProperty("encontrado")
        private boolean encontrado;
        @JsonProperty("description")
        private String description;
        @JsonProperty("category_code")
        private String categoryCode;
        @JsonProperty("category_name")
        private String categoryName;
        @JsonProperty("subcategory_code")
        private String subcategoryCode;
        @JsonProperty("subcategory_name")
        private String subcategoryName;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class RmsItemSupplierSection {
        @JsonProperty("encontrado")
        private boolean encontrado;
        @JsonProperty("supplier_code")
        private String supplierCode;
        @JsonProperty("provider_name")
        private String providerName;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class ProviderConfigEntry {
        @JsonProperty("proveedor_key")
        private String proveedorKey;
        @JsonProperty("config_key")
        private String configKey;
        @JsonProperty("config_valor")
        private String configValor;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class ProviderWsEntry {
        @JsonProperty("id_ws")
        private Long idWs;
        @JsonProperty("proveedor_key")
        private String proveedorKey;
        @JsonProperty("ws_key")
        private String wsKey;
        @JsonProperty("enabled")
        private String enabled;
        @JsonProperty("url")
        private String url;
        @JsonProperty("nombre_operacion")
        private String nombreOperacion;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class ProviderWsDefsEntry {
        @JsonProperty("proveedor_key")
        private String proveedorKey;
        @JsonProperty("ws_key")
        private String wsKey;
        @JsonProperty("default_clave")
        private String defaultClave;
        @JsonProperty("default_valor_text")
        private String defaultValorText;
        @JsonProperty("default_valor_num")
        private String defaultValorNum;
        @JsonProperty("tipo_def")
        private String tipoDef;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class InputFieldEntry {
        @JsonProperty("category_code")
        private String categoryCode;
        @JsonProperty("subcategory_code")
        private String subcategoryCode;
        @JsonProperty("service_provider_code")
        private String serviceProviderCode;
        @JsonProperty("field_id")
        private String fieldId;
        @JsonProperty("label")
        private String label;
        @JsonProperty("field_type")
        private String fieldType;
        @JsonProperty("capability")
        private String capability;
        @JsonProperty("is_required")
        private Integer isRequired;
        @JsonProperty("field_group")
        private String fieldGroup;
        @JsonProperty("field_order")
        private Integer fieldOrder;
    }
}

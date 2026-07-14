package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.AdServicioParametrosSection;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.InputFieldEntry;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.ProviderConfigEntry;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.ProviderWsDefsEntry;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.ProviderWsEntry;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.RmsItemMasterSection;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.RmsItemSupplierSection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador que consulta toda la parametrizacion asociada a un rms_item_code
 * en las distintas tablas de las BDs RMS y TUKUNAFUNC.
 * Solo se activa cuando ambos datasources estan configurados.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.datasource.prod.url")
public class OracleItemConfigDiagnosticAdapter {

    private final NamedParameterJdbcTemplate prodJdbc;
    private final NamedParameterJdbcTemplate rmsJdbc;

    public OracleItemConfigDiagnosticAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate prodJdbc,
            @Qualifier("rmsOracleJdbcTemplate") NamedParameterJdbcTemplate rmsJdbc) {
        this.prodJdbc = prodJdbc;
        this.rmsJdbc = rmsJdbc;
    }

    // ----------------------------------------------------------------
    // 1) AD_SERVICIO_PARAMETROS + AD_CANAL_SERVICIO (BD RMS, schema gpf_omnistack)
    // ----------------------------------------------------------------
    private static final String AD_SERVICIO_PARAMS_SQL = """
            SELECT
                TO_CHAR(sp.TERCERO)     AS service_provider_code,
                sp.ID_CONFIG            AS id_config,
                cs.ACTIVO               AS activo_canal,
                cs.CODIGO_CANAL         AS canal_codigo,
                sp.FLG_ITEM,
                sp.MONTO_MIN,
                sp.MONTO_MAX,
                sp.FLG_PAGO_MIXTO,
                sp.FLG_DEVOLUCION,
                sp.TIMEOUT_WS_MAX,
                sp.RETRIES_WS_MAX,
                sp.NUM_TICKETS,
                sp.REQUIERE_CONSENTIMIENTO
            FROM gpf_omnistack.AD_SERVICIO_PARAMETROS sp
            LEFT JOIN gpf_omnistack.AD_CANAL_SERVICIO cs
                ON cs.ID_CONFIG = sp.ID_CONFIG
               AND cs.CODIGO_CANAL = 1
            WHERE TRIM(sp.CODIGO_ITEM_RMS) = :rms_item_code
            """;

    public AdServicioParametrosSection queryAdServicioParametros(String rmsItemCode) {
        MapSqlParameterSource params = new MapSqlParameterSource("rms_item_code", rmsItemCode);
        List<AdServicioParametrosSection> results = rmsJdbc.query(AD_SERVICIO_PARAMS_SQL, params, (rs, rowNum) ->
                AdServicioParametrosSection.builder()
                        .encontrado(true)
                        .serviceProviderCode(rs.getString("service_provider_code"))
                        .idConfig(rs.getLong("id_config"))
                        .activoEnCanal("S".equalsIgnoreCase(rs.getString("activo_canal")))
                        .canalCodigo(rs.getObject("canal_codigo") != null ? rs.getInt("canal_codigo") : null)
                        .flgItem(rs.getString("FLG_ITEM"))
                        .montoMin(rs.getString("MONTO_MIN"))
                        .montoMax(rs.getString("MONTO_MAX"))
                        .flgPagoMixto(rs.getString("FLG_PAGO_MIXTO"))
                        .flgDevolucion(rs.getString("FLG_DEVOLUCION"))
                        .timeoutWsMax(rs.getString("TIMEOUT_WS_MAX"))
                        .retriesWsMax(rs.getString("RETRIES_WS_MAX"))
                        .numTickets(rs.getString("NUM_TICKETS"))
                        .requiereConsentimiento(rs.getString("REQUIERE_CONSENTIMIENTO"))
                        .build());
        if (results.isEmpty()) {
            return AdServicioParametrosSection.builder().encontrado(false).build();
        }
        return results.get(0);
    }

    // ----------------------------------------------------------------
    // 2) ITEM_MASTER + CLASS + SUBCLASS (BD RMS)
    // ----------------------------------------------------------------
    private static final String RMS_ITEM_MASTER_SQL = """
            SELECT
                im.ITEM_DESC         AS description,
                TO_CHAR(c.CLASS)     AS category_code,
                c.CLASS_NAME         AS category_name,
                TO_CHAR(sc.SUBCLASS) AS subcategory_code,
                sc.SUB_NAME          AS subcategory_name
            FROM ITEM_MASTER im
            JOIN CLASS c       ON c.CLASS = im.CLASS
            JOIN SUBCLASS sc   ON sc.CLASS = im.CLASS AND sc.SUBCLASS = im.SUBCLASS
            WHERE TRIM(im.ITEM) = :rms_item_code
            """;

    public RmsItemMasterSection queryRmsItemMaster(String rmsItemCode) {
        MapSqlParameterSource params = new MapSqlParameterSource("rms_item_code", rmsItemCode);
        List<RmsItemMasterSection> results = rmsJdbc.query(RMS_ITEM_MASTER_SQL, params, (rs, rowNum) ->
                RmsItemMasterSection.builder()
                        .encontrado(true)
                        .description(rs.getString("description"))
                        .categoryCode(rs.getString("category_code"))
                        .categoryName(rs.getString("category_name"))
                        .subcategoryCode(rs.getString("subcategory_code"))
                        .subcategoryName(rs.getString("subcategory_name"))
                        .build());
        if (results.isEmpty()) {
            return RmsItemMasterSection.builder().encontrado(false).build();
        }
        return results.get(0);
    }

    // ----------------------------------------------------------------
    // 3) ITEM_SUPPLIER + SUPS (BD RMS)
    // ----------------------------------------------------------------
    private static final String RMS_ITEM_SUPPLIER_SQL = """
            SELECT
                TO_CHAR(isup.SUPPLIER) AS supplier_code,
                s.SUP_NAME             AS provider_name
            FROM ITEM_SUPPLIER isup
            JOIN SUPS s ON s.SUPPLIER = isup.SUPPLIER
            WHERE TRIM(isup.ITEM) = :rms_item_code
            """;

    public RmsItemSupplierSection queryRmsItemSupplier(String rmsItemCode) {
        MapSqlParameterSource params = new MapSqlParameterSource("rms_item_code", rmsItemCode);
        List<RmsItemSupplierSection> results = rmsJdbc.query(RMS_ITEM_SUPPLIER_SQL, params, (rs, rowNum) ->
                RmsItemSupplierSection.builder()
                        .encontrado(true)
                        .supplierCode(rs.getString("supplier_code"))
                        .providerName(rs.getString("provider_name"))
                        .build());
        if (results.isEmpty()) {
            return RmsItemSupplierSection.builder().encontrado(false).build();
        }
        return results.get(0);
    }

    // ----------------------------------------------------------------
    // 4) IN_OMNI_PROVEEDOR_CONFIG (BD TUKUNAFUNC)
    //    Busca por el service_provider_code del item resuelto desde AD_SERVICIO_PARAMETROS.
    // ----------------------------------------------------------------
    private static final String PROVEEDOR_CONFIG_SQL = """
            SELECT cfg.PROVEEDOR_KEY, cfg.CONFIG_KEY, cfg.CONFIG_VALOR
            FROM IN_OMNI_PROVEEDOR_CONFIG cfg
            WHERE cfg.PROVEEDOR_KEY IN (
                SELECT c2.PROVEEDOR_KEY FROM IN_OMNI_PROVEEDOR_CONFIG c2
                WHERE c2.CONFIG_KEY = 'service_provider_code' AND c2.CONFIG_VALOR = :service_provider_code
            )
            ORDER BY cfg.PROVEEDOR_KEY, cfg.CONFIG_KEY
            """;

    public List<ProviderConfigEntry> queryProviderConfig(String serviceProviderCode) {
        if (serviceProviderCode == null) return List.of();
        MapSqlParameterSource params = new MapSqlParameterSource("service_provider_code", serviceProviderCode);
        return prodJdbc.query(PROVEEDOR_CONFIG_SQL, params, (rs, rowNum) ->
                ProviderConfigEntry.builder()
                        .proveedorKey(rs.getString("PROVEEDOR_KEY"))
                        .configKey(rs.getString("CONFIG_KEY"))
                        .configValor(rs.getString("CONFIG_VALOR"))
                        .build());
    }

    // ----------------------------------------------------------------
    // 5) IN_OMNI_PROVEEDOR_WS (BD TUKUNAFUNC)
    //    Solo operaciones que tienen este item configurado en WS_DEFS
    //    (formato multi-item o legacy single-item).
    // ----------------------------------------------------------------
    private static final String PROVEEDOR_WS_SQL = """
            SELECT ws.ID_WS, ws.PROVEEDOR_KEY, ws.WS_KEY, ws.ENABLED, ws.URL, ws.NOMBRE_OPERACION
            FROM IN_OMNI_PROVEEDOR_WS ws
            WHERE ws.PROVEEDOR_KEY IN (
                SELECT c2.PROVEEDOR_KEY FROM IN_OMNI_PROVEEDOR_CONFIG c2
                WHERE c2.CONFIG_KEY = 'service_provider_code' AND c2.CONFIG_VALOR = :service_provider_code
            )
            AND EXISTS (
                SELECT 1 FROM IN_OMNI_PROVEEDOR_WS_DEFS d
                WHERE d.ID_WS = ws.ID_WS
                  AND (d.DEFAULT_CLAVE = :item_key
                       OR (d.DEFAULT_CLAVE = 'item' AND d.DEFAULT_VALOR_TEXT = :rms_item_code))
            )
            ORDER BY ws.PROVEEDOR_KEY, ws.WS_KEY
            """;

    public List<ProviderWsEntry> queryProviderWs(String serviceProviderCode, String rmsItemCode) {
        if (serviceProviderCode == null) return List.of();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("service_provider_code", serviceProviderCode)
                .addValue("item_key", "item." + rmsItemCode)
                .addValue("rms_item_code", rmsItemCode);
        return prodJdbc.query(PROVEEDOR_WS_SQL, params, (rs, rowNum) ->
                ProviderWsEntry.builder()
                        .idWs(rs.getLong("ID_WS"))
                        .proveedorKey(rs.getString("PROVEEDOR_KEY"))
                        .wsKey(rs.getString("WS_KEY"))
                        .enabled(rs.getString("ENABLED"))
                        .url(rs.getString("URL"))
                        .nombreOperacion(rs.getString("NOMBRE_OPERACION"))
                        .build());
    }

    // ----------------------------------------------------------------
    // 6) IN_OMNI_PROVEEDOR_WS_DEFS (BD TUKUNAFUNC)
    //    Solo definiciones que referencian este item especifico
    //    (formato multi-item: item.{rmsItemCode} o legacy: item={rmsItemCode}).
    // ----------------------------------------------------------------
    private static final String PROVEEDOR_WS_DEFS_SQL = """
            SELECT ws.PROVEEDOR_KEY, ws.WS_KEY,
                   d.DEFAULT_CLAVE, d.DEFAULT_VALOR_TEXT,
                   TO_CHAR(d.DEFAULT_VALOR_NUM) AS DEFAULT_VALOR_NUM, d.TIPO_DEF
            FROM IN_OMNI_PROVEEDOR_WS ws
            JOIN IN_OMNI_PROVEEDOR_WS_DEFS d ON d.ID_WS = ws.ID_WS
            WHERE ws.PROVEEDOR_KEY IN (
                SELECT c2.PROVEEDOR_KEY FROM IN_OMNI_PROVEEDOR_CONFIG c2
                WHERE c2.CONFIG_KEY = 'service_provider_code' AND c2.CONFIG_VALOR = :service_provider_code
            )
            AND (d.DEFAULT_CLAVE = :item_key
                 OR (d.DEFAULT_CLAVE = 'item' AND d.DEFAULT_VALOR_TEXT = :rms_item_code))
            ORDER BY ws.WS_KEY, d.DEFAULT_CLAVE
            """;

    public List<ProviderWsDefsEntry> queryProviderWsDefs(String serviceProviderCode, String rmsItemCode) {
        if (serviceProviderCode == null) return List.of();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("service_provider_code", serviceProviderCode)
                .addValue("item_key", "item." + rmsItemCode)
                .addValue("rms_item_code", rmsItemCode);
        return prodJdbc.query(PROVEEDOR_WS_DEFS_SQL, params, (rs, rowNum) ->
                ProviderWsDefsEntry.builder()
                        .proveedorKey(rs.getString("PROVEEDOR_KEY"))
                        .wsKey(rs.getString("WS_KEY"))
                        .defaultClave(rs.getString("DEFAULT_CLAVE"))
                        .defaultValorText(rs.getString("DEFAULT_VALOR_TEXT"))
                        .defaultValorNum(rs.getString("DEFAULT_VALOR_NUM"))
                        .tipoDef(rs.getString("TIPO_DEF"))
                        .build());
    }

    // ----------------------------------------------------------------
    // 7) IN_OMNI_INPUT_FIELDS (BD TUKUNAFUNC)
    // ----------------------------------------------------------------
    private static final String INPUT_FIELDS_SQL = """
            SELECT CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE,
                   FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, FIELD_ORDER
            FROM IN_OMNI_INPUT_FIELDS
            WHERE RMS_ITEM_CODE = :rms_item_code AND ENABLED = 'S'
            ORDER BY CAPABILITY, FIELD_ORDER
            """;

    public List<InputFieldEntry> queryInputFields(String rmsItemCode) {
        MapSqlParameterSource params = new MapSqlParameterSource("rms_item_code", rmsItemCode);
        return prodJdbc.query(INPUT_FIELDS_SQL, params, (rs, rowNum) ->
                InputFieldEntry.builder()
                        .categoryCode(rs.getString("CATEGORY_CODE"))
                        .subcategoryCode(rs.getString("SUBCATEGORY_CODE"))
                        .serviceProviderCode(rs.getString("SERVICE_PROVIDER_CODE"))
                        .fieldId(rs.getString("FIELD_ID"))
                        .label(rs.getString("LABEL"))
                        .fieldType(rs.getString("FIELD_TYPE"))
                        .capability(rs.getString("CAPABILITY"))
                        .isRequired(rs.getInt("IS_REQUIRED"))
                        .fieldGroup(rs.getString("FIELD_GROUP"))
                        .fieldOrder(rs.getInt("FIELD_ORDER"))
                        .build());
    }
}

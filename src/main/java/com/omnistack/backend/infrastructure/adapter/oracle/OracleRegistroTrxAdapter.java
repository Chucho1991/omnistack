package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.RegistroTrxPort;
import com.omnistack.backend.domain.model.RegistroTrx;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.datasource.prod.url")
public class OracleRegistroTrxAdapter implements RegistroTrxPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleRegistroTrxAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${app.datasource.prod.schema:TUKUNAFUNC}")
    private String schema;

    private String insertSql;

    @PostConstruct
    void init() {
        insertSql = "INSERT INTO " + schema + ".IN_OMNI_REGISTRO_TRX "
                + "(CODIGO, UUID, CADENA, FARMACIA, NOMBRE_FARMACIA, POS, CANAL, "
                + " PROVEEDOR, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE, "
                + " CAPABILITY, AUTHORIZATION, MONTO, MONEDA, COD_ESTADO, ES_ERROR) "
                + "VALUES "
                + "(SEQ_IN_OMNI_REGISTRO_TRX.NEXTVAL, :uuid, :cadena, :farmacia, :nombreFarmacia, :pos, :canal, "
                + " :proveedor, :categoryCode, :subcategoryCode, :serviceProviderCode, :rmsItemCode, "
                + " :capability, :authorization, :monto, :moneda, :codEstado, 'N')";
    }

    @Override
    @Async("loggingExecutor")
    public void save(RegistroTrx entry) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("uuid", entry.getUuid())
                    .addValue("cadena", parseNumber(entry.getChain()))
                    .addValue("farmacia", parseNumber(entry.getStore()))
                    .addValue("nombreFarmacia", entry.getStoreName())
                    .addValue("pos", entry.getPos())
                    .addValue("canal", entry.getCanal())
                    .addValue("proveedor", entry.getProveedor())
                    .addValue("categoryCode", entry.getCategoryCode())
                    .addValue("subcategoryCode", entry.getSubcategoryCode())
                    .addValue("serviceProviderCode", entry.getServiceProviderCode())
                    .addValue("rmsItemCode", entry.getRmsItemCode())
                    .addValue("capability", entry.getCapability())
                    .addValue("authorization", entry.getAuthorization())
                    .addValue("monto", entry.getMonto())
                    .addValue("moneda", entry.getMoneda() != null ? entry.getMoneda() : "USD")
                    .addValue("codEstado", entry.getCodEstado());
            jdbcTemplate.update(insertSql, params);
        } catch (Exception ex) {
            log.warn("Error al registrar REGISTRO_TRX uuid={}: {}", entry.getUuid(), ex.getMessage(), ex);
        }
    }

    private Long parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

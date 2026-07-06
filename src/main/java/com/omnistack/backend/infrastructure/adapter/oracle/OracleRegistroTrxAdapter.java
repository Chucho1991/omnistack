package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.RegistroTrxPort;
import com.omnistack.backend.domain.model.RegistroTrx;
import jakarta.annotation.PostConstruct;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
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
    private String findAuthByHomologatedCodeSql;

    @PostConstruct
    void init() {
        insertSql = "INSERT INTO " + schema + ".IN_OMNI_REGISTRO_TRX "
                + "(CODIGO, UUID, CADENA, FARMACIA, NOMBRE_FARMACIA, POS, CANAL, "
                + " PROVEEDOR, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE, "
                + " CAPABILITY, AUTHORIZATION, MONTO, MONEDA, COD_ESTADO, ES_ERROR, CP_VAR1) "
                + "VALUES "
                + "((SELECT NVL(MAX(CODIGO), 0) + 1 FROM " + schema + ".IN_OMNI_REGISTRO_TRX), :uuid, :cadena, :farmacia, :nombreFarmacia, :pos, :canal, "
                + " :proveedor, :categoryCode, :subcategoryCode, :serviceProviderCode, :rmsItemCode, "
                + " :capability, :authorization, :monto, :moneda, :codEstado, 'N', :cpVar1)";

        findAuthByHomologatedCodeSql = "SELECT AUTHORIZATION FROM " + schema + ".IN_OMNI_REGISTRO_TRX "
                + "WHERE CP_VAR1 = :homologatedCode AND ROWNUM = 1 ORDER BY CODIGO DESC";
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
                    .addValue("codEstado", entry.getCodEstado())
                    .addValue("cpVar1", entry.getCpVar1());
            jdbcTemplate.update(insertSql, params);
        } catch (Exception ex) {
            log.warn("Error al registrar REGISTRO_TRX uuid={}: {}", entry.getUuid(), ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<String> findOriginalAuthByHomologatedCode(String homologatedCode) {
        if (homologatedCode == null || homologatedCode.isBlank()) {
            return Optional.empty();
        }
        try {
            String authorization = jdbcTemplate.queryForObject(
                    findAuthByHomologatedCodeSql,
                    new MapSqlParameterSource("homologatedCode", homologatedCode),
                    String.class);
            return Optional.ofNullable(authorization);
        } catch (EmptyResultDataAccessException ex) {
            log.debug("No se encontro AUTHORIZATION para codigo homologado={}", homologatedCode);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Error al consultar AUTHORIZATION por codigo homologado={}: {}", homologatedCode, ex.getMessage());
            return Optional.empty();
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

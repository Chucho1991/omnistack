package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.AuditLogPort;
import com.omnistack.backend.domain.enums.TransactionStatus;
import com.omnistack.backend.domain.model.TransactionAuditLog;
import jakarta.annotation.PostConstruct;
import java.sql.Types;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "app.datasource.prod.url")
public class OracleAuditLogAdapter implements AuditLogPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleAuditLogAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${app.datasource.prod.schema:TUKUNAFUNC}")
    private String schema;

    private String insertSql;

    @PostConstruct
    void init() {
        insertSql = "INSERT INTO " + schema + ".IN_OMNI_LOGS_APP "
                + "(CODIGO, UUID, REQUEST, RESPONSE, USUARIO, PROVEEDOR, CAPABILITY, CANAL, "
                + " CADENA, FARMACIA, NOMBRE_FARMACIA, POS, URL, METODO, "
                + " HTTP_STATUS, ES_ERROR, COD_ERROR, MSG_ERROR) "
                + "VALUES "
                + "(SEQ_IN_OMNI_LOGS_APP.NEXTVAL, :uuid, :request, :response, :usuario, :proveedor, :capability, :canal, "
                + " :cadena, :farmacia, :nombreFarmacia, :pos, :url, :metodo, "
                + " :httpStatus, :esError, :codError, :msgError)";
        log.info("OracleAuditLogAdapter init — schema={}", schema);
    }

    @Override
    @Async("loggingExecutor")
    public void save(TransactionAuditLog entry) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("uuid", entry.getUuid())
                    .addValue("request", entry.getInternalRequest(), Types.CLOB)
                    .addValue("response", entry.getInternalResponse(), Types.CLOB)
                    .addValue("usuario", entry.getTechnicalUser())
                    .addValue("proveedor", entry.getExternalProvider())
                    .addValue("capability", entry.getCapability())
                    .addValue("canal", entry.getCanal())
                    .addValue("cadena", parseNumber(entry.getChain()))
                    .addValue("farmacia", parseNumber(entry.getStore()))
                    .addValue("nombreFarmacia", entry.getStoreName())
                    .addValue("pos", entry.getPos())
                    .addValue("url", entry.getEndpointInvoked())
                    .addValue("metodo", "POST")
                    .addValue("httpStatus", entry.getHttpStatus())
                    .addValue("esError", entry.getStatus() == TransactionStatus.FAILED ? "S" : "N")
                    .addValue("codError", entry.getErrorCode())
                    .addValue("msgError", truncate(entry.getErrorMessage(), 2000));
            jdbcTemplate.update(insertSql, params);
        } catch (Exception ex) {
            log.warn("Error al registrar LOGS_APP uuid={}: {}", entry.getUuid(), ex.getMessage(), ex);
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

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}

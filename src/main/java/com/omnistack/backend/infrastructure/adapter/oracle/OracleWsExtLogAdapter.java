package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.WsExtLogPort;
import com.omnistack.backend.domain.model.ProviderCallLog;
import jakarta.annotation.PostConstruct;
import java.sql.Types;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.datasource.prod.url")
public class OracleWsExtLogAdapter implements WsExtLogPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleWsExtLogAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${app.datasource.prod.schema:TUKUNAFUNC}")
    private String schema;

    private String insertSql;

    @PostConstruct
    void init() {
        insertSql = "INSERT INTO " + schema + ".IN_OMNI_LOGS_WS_EXT "
                + "(CODIGO, UUID, PROVEEDOR, WS_KEY, URL, REQUEST, RESPONSE, DURACION_MS, HTTP_STATUS, ES_ERROR, CP_VAR1) "
                + "VALUES "
                + "(" + schema + ".SEQ_IN_OMNI_LOGS_WS_EXT.NEXTVAL, :uuid, :proveedor, :wsKey, :url, :request, :response, :duracionMs, :httpStatus, :esError, :cpVar1)";
        log.info("OracleWsExtLogAdapter init — schema={}", schema);
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM all_objects WHERE owner=:own AND object_name='IN_OMNI_LOGS_WS_EXT' AND object_type='TABLE'",
                    new MapSqlParameterSource().addValue("own", schema.toUpperCase()),
                    Integer.class);
            log.info("OracleWsExtLogAdapter diagnostic — IN_OMNI_LOGS_WS_EXT visible via JDBC: count={}", cnt);
            Integer sessCnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_objects WHERE object_name='IN_OMNI_LOGS_WS_EXT' AND object_type='TABLE'",
                    new MapSqlParameterSource(), Integer.class);
            log.info("OracleWsExtLogAdapter diagnostic — IN_OMNI_LOGS_WS_EXT in user_objects: count={}", sessCnt);
            String sesUser = jdbcTemplate.queryForObject(
                    "SELECT USER FROM DUAL", new MapSqlParameterSource(), String.class);
            log.info("OracleWsExtLogAdapter diagnostic — connected as user: {}", sesUser);
        } catch (Exception e) {
            log.warn("OracleWsExtLogAdapter diagnostic failed: {}", e.getMessage());
        }
    }

    @Override
    public void log(ProviderCallLog entry) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("uuid", entry.getUuid())
                .addValue("proveedor", entry.getProviderKey())
                .addValue("wsKey", entry.getWsKey())
                .addValue("url", truncate(entry.getUrl(), 500))
                .addValue("request", entry.getRequestJson(), Types.CLOB)
                .addValue("response", entry.getResponseJson(), Types.CLOB)
                .addValue("duracionMs", entry.getDurationMs())
                .addValue("httpStatus", entry.getHttpStatus())
                .addValue("esError", entry.isError() ? "S" : "N")
                .addValue("cpVar1", truncate(entry.getErrorMessage(), 1500));
        jdbcTemplate.update(insertSql, params);
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}

package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.ProviderWsDefsPort;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacion Oracle de ProviderWsDefsPort.
 * Lee IN_OMNI_PROVEEDOR_WS_DEFS JOIN IN_OMNI_PROVEEDOR_WS del schema GPF_OMNISTACK.
 * Solo se activa cuando app.datasource.prod.url esta configurado.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.datasource.prod.url")
public class OracleProviderWsDefsAdapter implements ProviderWsDefsPort {

    private static final String LOAD_ALL_SQL =
            "SELECT LOWER(w.PROVEEDOR_KEY) || '|' || UPPER(w.WS_KEY) || '|' || LOWER(d.DEFAULT_CLAVE) AS cache_key, "
            + "       COALESCE(d.DEFAULT_VALOR_TEXT, TO_CHAR(d.DEFAULT_VALOR_NUM)) AS valor "
            + "FROM IN_OMNI_PROVEEDOR_WS w "
            + "JOIN IN_OMNI_PROVEEDOR_WS_DEFS d ON d.ID_WS = w.ID_WS "
            + "WHERE w.ENABLED = 'S'";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleProviderWsDefsAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> loadAll() {
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query(LOAD_ALL_SQL, EmptySqlParameterSource.INSTANCE, rs -> {
            result.put(rs.getString("cache_key"), rs.getString("valor"));
        });
        log.info("ProviderWsDefs: {} entradas cargadas desde IN_OMNI_PROVEEDOR_WS_DEFS", result.size());
        return result;
    }
}

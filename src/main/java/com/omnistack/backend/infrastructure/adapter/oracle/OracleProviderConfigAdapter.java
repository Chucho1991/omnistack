package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.ProviderConfigPort;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacion Oracle de ProviderConfigPort.
 * Lee la tabla IN_OMNI_PROVEEDOR_CONFIG del schema GPF_OMNISTACK.
 * Solo se activa cuando app.datasource.prod.url esta configurado.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.datasource.prod.url")
public class OracleProviderConfigAdapter implements ProviderConfigPort {

    private static final String LOAD_ALL_SQL =
            "SELECT LOWER(PROVEEDOR_KEY) || '|' || LOWER(CONFIG_KEY) AS cache_key, CONFIG_VALOR "
            + "FROM IN_OMNI_PROVEEDOR_CONFIG";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleProviderConfigAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> loadAll() {
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query(LOAD_ALL_SQL, EmptySqlParameterSource.INSTANCE, rs -> {
            result.put(rs.getString("cache_key"), rs.getString("CONFIG_VALOR"));
        });
        log.info("ProviderConfig: {} entradas cargadas desde IN_OMNI_PROVEEDOR_CONFIG", result.size());
        return result;
    }
}

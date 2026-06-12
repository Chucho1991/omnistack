package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.ProviderWsPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacion Oracle de ProviderWsPort.
 * Lee la tabla IN_OMNI_PROVEEDOR_WS del schema GPF_OMNISTACK.
 * Solo se activa cuando app.datasource.prod.url esta configurado.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.datasource.prod.url")
public class OracleProviderWsAdapter implements ProviderWsPort {

    private static final String LOAD_ALL_SQL =
            "SELECT LOWER(PROVEEDOR_KEY) || '|' || UPPER(WS_KEY) AS cache_key, URL "
            + "FROM IN_OMNI_PROVEEDOR_WS "
            + "WHERE ENABLED = 'S' AND URL IS NOT NULL";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleProviderWsAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> loadAll() {
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query(LOAD_ALL_SQL, EmptySqlParameterSource.INSTANCE, rs -> {
            result.put(rs.getString("cache_key"), rs.getString("URL"));
        });
        log.info("ProviderWs: {} URLs cargadas desde IN_OMNI_PROVEEDOR_WS", result.size());
        return result;
    }

    @Override
    public Optional<String> findUrl(String providerKey, String wsKey) {
        return Optional.ofNullable(loadAll().get(cacheKey(providerKey, wsKey)));
    }

    private static String cacheKey(String providerKey, String wsKey) {
        return providerKey.toLowerCase() + "|" + wsKey.toUpperCase();
    }
}

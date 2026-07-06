package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.AdItemServicioPort;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacion Oracle de AdItemServicioPort.
 * Lee AD_ITEM_SERVICIO join AD_SERVICIO_PARAMETROS para obtener los TAGs por rms_item_code.
 * Clave de cache: "rms_item_code|tag_lowercase" → valor_tag.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.datasource.rms.url")
public class OracleAdItemServicioAdapter implements AdItemServicioPort {

    private static final String LOAD_ALL_SQL =
            "SELECT TRIM(sp.CODIGO_ITEM_RMS) AS rms_item_code, "
            + "LOWER(ai.TAG) AS tag, "
            + "ai.VALOR_TAG "
            + "FROM gpf_omnistack.AD_ITEM_SERVICIO ai "
            + "JOIN gpf_omnistack.AD_SERVICIO_PARAMETROS sp ON sp.ID_CONFIG = ai.ID_CONFIG "
            + "WHERE ai.ACTIVO = 'S' "
            + "AND ai.TAG IS NOT NULL "
            + "AND ai.VALOR_TAG IS NOT NULL";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleAdItemServicioAdapter(
            @Qualifier("rmsOracleJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> loadAll() {
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query(LOAD_ALL_SQL, EmptySqlParameterSource.INSTANCE, rs -> {
            String key = rs.getString("rms_item_code") + "|" + rs.getString("tag");
            result.put(key, rs.getString("VALOR_TAG"));
        });
        log.info("AdItemServicio: {} entradas cargadas desde AD_ITEM_SERVICIO", result.size());
        return result;
    }
}

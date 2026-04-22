package com.omnistack.backend.config;

import com.omnistack.backend.config.properties.AppProperties;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Configuracion de conectividad Oracle para business-lines.
 */
@Configuration
public class BusinessLinesOracleConfig {

    /**
     * Registra una fuente de tiempo injectable para caches y pruebas.
     *
     * @return reloj del sistema
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Construye el datasource Oracle definido para business-lines.
     *
     * @param appProperties propiedades de aplicacion
     * @return datasource configurado
     */
    @Bean(name = "businessLinesOracleDataSource")
    public DataSource businessLinesOracleDataSource(AppProperties appProperties) {
        AppProperties.BusinessLines.Oracle.Datasource datasource = appProperties.getBusinessLines().getOracle().getDatasource1();
        return DataSourceBuilder.create()
                .url(datasource.getUrl())
                .username(datasource.getUsername())
                .password(datasource.getPassword())
                .driverClassName(datasource.getDriverClassName())
                .build();
    }

    /**
     * Expone el JDBC template con parametros nombrados para la lectura de catalogos.
     *
     * @param businessLinesOracleDataSource datasource Oracle del endpoint
     * @return jdbc template nombrado
     */
    @Bean(name = "businessLinesOracleNamedParameterJdbcTemplate")
    @Primary
    public NamedParameterJdbcTemplate businessLinesOracleNamedParameterJdbcTemplate(
            @org.springframework.beans.factory.annotation.Qualifier("businessLinesOracleDataSource")
            DataSource businessLinesOracleDataSource) {
        return new NamedParameterJdbcTemplate(businessLinesOracleDataSource);
    }
}

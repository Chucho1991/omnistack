package com.omnistack.backend.config;

import com.omnistack.backend.config.properties.AppProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Datasource Oracle RMS — items maestros (ITEM_MASTER, CLASS, SUBCLASS, SUPS, UDA_*).
 * En dev apunta al schema rms del Docker local; en QA apunta a gpf_lectura@momqa.
 * Se activa cuando app.datasource.rms.url esta definido.
 */
@Configuration
@ConditionalOnProperty(name = "app.datasource.rms.url")
public class RmsOracleConfig {

    @Bean(name = "rmsOracleDataSource")
    public DataSource rmsOracleDataSource(AppProperties appProperties) {
        AppProperties.Datasource.OracleDatasource ds = appProperties.getDatasource().getRms();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ds.getUrl());
        config.setUsername(ds.getUsername());
        config.setPassword(ds.getPassword());
        config.setDriverClassName(ds.getDriverClassName());
        config.setPoolName("omni-rms-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setLeakDetectionThreshold(30_000);
        return new HikariDataSource(config);
    }

    @Bean(name = "rmsOracleJdbcTemplate")
    public NamedParameterJdbcTemplate rmsOracleJdbcTemplate(
            @Qualifier("rmsOracleDataSource") DataSource rmsOracleDataSource) {
        return new NamedParameterJdbcTemplate(rmsOracleDataSource);
    }
}

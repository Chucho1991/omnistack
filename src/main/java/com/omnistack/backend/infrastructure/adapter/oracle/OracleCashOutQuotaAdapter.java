package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.CashOutQuotaPort;
import com.omnistack.backend.domain.enums.CashOutQuotaStatus;
import com.omnistack.backend.domain.model.CashOutQuotaEntry;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador Oracle para la bitacora de cupos diarios CASH_OUT.
 * Opera sobre la tabla TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.datasource.prod.url")
public class OracleCashOutQuotaAdapter implements CashOutQuotaPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${app.datasource.prod.schema:TUKUNAFUNC}")
    private String schema;

    private String getConsumedQuotaSql;
    private String insertReservationSql;
    private String confirmReservationSql;
    private String revertReservationSql;
    private String expireStaleReservationsSql;
    private String findByUuidSql;

    public OracleCashOutQuotaAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void init() {
        getConsumedQuotaSql = """
                SELECT NVL(SUM(MONTO), 0) AS consumed
                FROM %s.IN_OMNI_CASHOUT_CUPO_DIARIO
                WHERE CADENA = :cadena
                  AND FARMACIA = :farmacia
                  AND RMS_ITEM_CODE = :rmsItemCode
                  AND FECHA_OPERACION = :fechaOperacion
                  AND ESTADO IN ('RESERVADO', 'CONFIRMADO')
                """.formatted(schema);

        insertReservationSql = """
                INSERT INTO %s.IN_OMNI_CASHOUT_CUPO_DIARIO (
                    ID_CUPO, UUID, CADENA, FARMACIA, POS, RMS_ITEM_CODE, SERVICE_PROVIDER_CODE,
                    MONTO, ESTADO, FECHA_OPERACION, FECHA_RESERVA
                ) VALUES (
                    %s.SEQ_IN_OMNI_CASHOUT_CUPO.NEXTVAL,
                    :uuid, :cadena, :farmacia, :pos, :rmsItemCode, :serviceProviderCode,
                    :monto, 'RESERVADO', :fechaOperacion, SYSTIMESTAMP
                )
                """.formatted(schema, schema);

        confirmReservationSql = """
                UPDATE %s.IN_OMNI_CASHOUT_CUPO_DIARIO
                SET ESTADO = 'CONFIRMADO',
                    FECHA_CONFIRMACION = SYSTIMESTAMP,
                    USR_MODIFICACION = USER,
                    FEC_MODIFICACION = SYSDATE
                WHERE UUID = :uuid
                  AND ESTADO = 'RESERVADO'
                """.formatted(schema);

        revertReservationSql = """
                UPDATE %s.IN_OMNI_CASHOUT_CUPO_DIARIO
                SET ESTADO = 'REVERTIDO',
                    FECHA_REVERSION = SYSTIMESTAMP,
                    USR_MODIFICACION = USER,
                    FEC_MODIFICACION = SYSDATE
                WHERE UUID = :uuid
                  AND ESTADO IN ('RESERVADO', 'CONFIRMADO')
                  AND FECHA_OPERACION = :fechaOperacion
                """.formatted(schema);

        expireStaleReservationsSql = """
                UPDATE %s.IN_OMNI_CASHOUT_CUPO_DIARIO
                SET ESTADO = 'EXPIRADO',
                    FECHA_EXPIRACION = SYSTIMESTAMP,
                    USR_MODIFICACION = USER,
                    FEC_MODIFICACION = SYSDATE
                WHERE ESTADO = 'RESERVADO'
                  AND FECHA_RESERVA < :cutoffTimestamp
                """.formatted(schema);

        findByUuidSql = """
                SELECT ID_CUPO, UUID, CADENA, FARMACIA, POS, RMS_ITEM_CODE, SERVICE_PROVIDER_CODE,
                       MONTO, ESTADO, FECHA_OPERACION, FECHA_RESERVA, FECHA_CONFIRMACION,
                       FECHA_EXPIRACION, FECHA_REVERSION
                FROM %s.IN_OMNI_CASHOUT_CUPO_DIARIO
                WHERE UUID = :uuid
                """.formatted(schema);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getConsumedQuota(String chain, String store, String rmsItemCode, LocalDate operationDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cadena", parseNumber(chain))
                .addValue("farmacia", parseNumber(store))
                .addValue("rmsItemCode", rmsItemCode)
                .addValue("fechaOperacion", java.sql.Date.valueOf(operationDate));
        try {
            BigDecimal result = jdbcTemplate.queryForObject(getConsumedQuotaSql, params, BigDecimal.class);
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception ex) {
            log.error("Error al consultar cupo consumido chain={}, store={}, item={}, fecha={}: {}",
                    chain, store, rmsItemCode, operationDate, ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional
    public void saveReservation(CashOutQuotaEntry entry) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("uuid", entry.getUuid())
                .addValue("cadena", parseNumber(entry.getChain()))
                .addValue("farmacia", parseNumber(entry.getStore()))
                .addValue("pos", entry.getPos())
                .addValue("rmsItemCode", entry.getRmsItemCode())
                .addValue("serviceProviderCode", entry.getServiceProviderCode())
                .addValue("monto", entry.getAmount())
                .addValue("fechaOperacion", java.sql.Date.valueOf(entry.getOperationDate()));
        jdbcTemplate.update(insertReservationSql, params);
        log.debug("Reserva de cupo CASH_OUT registrada. uuid={}, store={}, monto={}, fecha={}",
                entry.getUuid(), entry.getStore(), entry.getAmount(), entry.getOperationDate());
    }

    @Override
    @Transactional
    public boolean confirmReservation(String uuid) {
        MapSqlParameterSource params = new MapSqlParameterSource("uuid", uuid);
        int updated = jdbcTemplate.update(confirmReservationSql, params);
        if (updated > 0) {
            log.debug("Reserva de cupo CASH_OUT confirmada. uuid={}", uuid);
        } else {
            log.warn("No se encontro reserva RESERVADO para confirmar. uuid={}", uuid);
        }
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean revertReservation(String uuid, LocalDate operationDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("fechaOperacion", java.sql.Date.valueOf(operationDate));
        int updated = jdbcTemplate.update(revertReservationSql, params);
        if (updated > 0) {
            log.debug("Reserva de cupo CASH_OUT revertida. uuid={}, fecha={}", uuid, operationDate);
        } else {
            log.debug("No se restituye cupo CASH_OUT (distinto dia o estado no aplica). uuid={}, fecha={}",
                    uuid, operationDate);
        }
        return updated > 0;
    }

    @Override
    @Transactional
    public int expireStaleReservations(LocalDateTime cutoffTimestamp) {
        MapSqlParameterSource params = new MapSqlParameterSource(
                "cutoffTimestamp", Timestamp.valueOf(cutoffTimestamp));
        int expired = jdbcTemplate.update(expireStaleReservationsSql, params);
        if (expired > 0) {
            log.info("Reservas de cupo CASH_OUT expiradas: count={}, cutoff={}", expired, cutoffTimestamp);
        }
        return expired;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashOutQuotaEntry> findByUuid(String uuid) {
        MapSqlParameterSource params = new MapSqlParameterSource("uuid", uuid);
        try {
            CashOutQuotaEntry entry = jdbcTemplate.queryForObject(findByUuidSql, params, quotaRowMapper());
            return Optional.ofNullable(entry);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private RowMapper<CashOutQuotaEntry> quotaRowMapper() {
        return (rs, rowNum) -> CashOutQuotaEntry.builder()
                .id(rs.getLong("ID_CUPO"))
                .uuid(rs.getString("UUID"))
                .chain(String.valueOf(rs.getLong("CADENA")))
                .store(String.valueOf(rs.getLong("FARMACIA")))
                .pos(rs.getString("POS"))
                .rmsItemCode(rs.getString("RMS_ITEM_CODE"))
                .serviceProviderCode(rs.getString("SERVICE_PROVIDER_CODE"))
                .amount(rs.getBigDecimal("MONTO"))
                .status(CashOutQuotaStatus.valueOf(rs.getString("ESTADO")))
                .operationDate(rs.getDate("FECHA_OPERACION").toLocalDate())
                .reservationTimestamp(toLocalDateTime(rs.getTimestamp("FECHA_RESERVA")))
                .confirmationTimestamp(toLocalDateTime(rs.getTimestamp("FECHA_CONFIRMACION")))
                .expirationTimestamp(toLocalDateTime(rs.getTimestamp("FECHA_EXPIRACION")))
                .reversalTimestamp(toLocalDateTime(rs.getTimestamp("FECHA_REVERSION")))
                .build();
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
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

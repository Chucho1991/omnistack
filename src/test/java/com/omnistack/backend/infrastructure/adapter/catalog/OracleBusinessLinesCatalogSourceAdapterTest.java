package com.omnistack.backend.infrastructure.adapter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.ChannelPos;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@SuppressWarnings("unchecked")
class OracleBusinessLinesCatalogSourceAdapterTest {

    @Test
    void shouldAssembleCatalogSnapshotFromOracleCatalogs() {
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        OracleBusinessLinesSqlProvider sqlProvider = Mockito.mock(OracleBusinessLinesSqlProvider.class);
        AppProperties appProperties = new AppProperties();
        OracleBusinessLinesCatalogSourceAdapter adapter = new OracleBusinessLinesCatalogSourceAdapter(jdbcTemplate, sqlProvider, appProperties);

        BusinessLinesRequest request = BusinessLinesRequest.builder()
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .build();

        when(sqlProvider.getCategorySubcategorySql()).thenReturn("category");
        when(sqlProvider.getServiceProvidersSql()).thenReturn("provider");
        when(sqlProvider.getServicesSql()).thenReturn("service");
        when(sqlProvider.getCapabilitiesSql()).thenReturn("capability");
        when(sqlProvider.getInputFieldsSql()).thenReturn("inputField");
        when(sqlProvider.getPaymentMethodsSql()).thenReturn("paymentMethod");

        when(jdbcTemplate.query(eq("category"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.CategorySubcategoryRow("1", "ENTRETENIMIENTO", "1", "PRONOSTICOS DEPORTIVOS", true)));
        when(jdbcTemplate.query(eq("provider"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.ServiceProviderRow("1", "1", "1", "9999999999001", "ECUABET", true)));
        when(jdbcTemplate.query(eq("service"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.ServiceRow(
                        "1",
                        "1",
                        "1",
                        "100713841",
                        "ECUABET CASH IN",
                        true,
                        "ABC1234",
                        "CASH_IN",
                        true,
                        "RECA",
                        true,
                        "1",
                        "200",
                        "10000",
                        "3",
                        "3",
                        true,
                        "<html>consent</html>")));
        when(jdbcTemplate.query(eq("capability"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.CapabilityRow("1", "1", "1", "100713841", "PRECHECK"),
                new OracleBusinessLinesCatalogSourceAdapter.CapabilityRow("1", "1", "1", "100713841", "CREATE_TICKET")));
        when(jdbcTemplate.query(eq("inputField"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.InputFieldRow(
                        "1",
                        "1",
                        "1",
                        "100713841",
                        "document",
                        "Documento Usuario",
                        "STRING",
                        "PRECHECK",
                        true,
                        "IDENTIFICATION",
                        "OR")));
        when(jdbcTemplate.query(eq("paymentMethod"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.PaymentMethodRow(
                        "1",
                        "1",
                        "1",
                        "100713841",
                        2,
                        "TARJETA CREDITO",
                        true)));

        var snapshot = adapter.loadCatalogSnapshot(request);

        assertEquals(1, snapshot.getCategories().size());
        assertEquals("ENTRETENIMIENTO", snapshot.getCategories().get(0).getCategoryName());
        assertEquals(1, snapshot.getCategories().get(0).getSubcategories().size());
        assertEquals(1, snapshot.getCategories().get(0).getSubcategories().get(0).getProviders().size());
        assertEquals("9999999999001", snapshot.getCategories().get(0).getSubcategories().get(0).getProviders().get(0).getRucProvider());
        assertEquals(1, snapshot.getServices().size());
        assertEquals(2, snapshot.getServices().get(0).getCapabilities().size());
        assertEquals("10000", snapshot.getServices().get(0).getTimeoutWsMax());
        assertEquals("3", snapshot.getServices().get(0).getRetriesWsMax());
        assertEquals("3", snapshot.getServices().get(0).getNumTickets());
        assertEquals("document", snapshot.getServices().get(0).getInputFields().get(0).getId());
        assertEquals("TARJETA_CREDITO", snapshot.getServices().get(0).getPaymentMethods().get(0).getPaymentMethodCode().name());
        assertTrue(snapshot.getServices().get(0).isRequiresConsent());
    }

    @Test
    void shouldExposeOnlyPrecheckInputFieldsForEcuabetCashOutCatalog() {
        OracleBusinessLinesSqlProvider sqlProvider = new OracleBusinessLinesSqlProvider();

        String inputFieldsSql = sqlProvider.getInputFieldsSql();

        assertTrue(inputFieldsSql.contains("'100708846' as rms_item_code, 'withdrawId' as input_field_id"));
        assertTrue(inputFieldsSql.contains(
                "'100708846' as rms_item_code, 'password' as input_field_id, 'Contrase\u00f1a asignado a retiro' as label"));
        assertTrue(inputFieldsSql.contains(
                "'100708846' as rms_item_code, 'amount' as input_field_id, 'Monto' as label, "
                        + "'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, "
                        + "'AMOUNT' as field_group"));
        assertTrue(inputFieldsSql.contains("'PASS' as field_group"));
        assertFalse(inputFieldsSql.contains(
                "'100708846' as rms_item_code, 'amount' as input_field_id, 'Monto reverso' as label"));
        assertFalse(inputFieldsSql.contains(
                "'100708846' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, "
                        + "'STRING' as field_type, 'REVERSE' as capability_code"));
        assertFalse(inputFieldsSql.contains(
                "'100708846' as rms_item_code, 'motivo' as input_field_id, 'Motivo del reverso' as label"));
        assertFalse(inputFieldsSql.contains(
                "'100708846' as rms_item_code, 'withdrawId' as input_field_id, 'Numero asignado a retiro' as label, "
                        + "'STRING' as field_type, 'REVERSE' as capability_code"));
        assertFalse(inputFieldsSql.contains(
                "'100708846' as rms_item_code, 'password' as input_field_id, 'Clave de retiro' as label, "
                        + "'STRING' as field_type, 'REVERSE' as capability_code"));
    }

    @Test
    void shouldExposeOnlyPrecheckInputFieldsForBet593CashOutCatalog() {
        OracleBusinessLinesSqlProvider sqlProvider = new OracleBusinessLinesSqlProvider();

        String inputFieldsSql = sqlProvider.getInputFieldsSql();

        assertTrue(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, "
                        + "'STRING' as field_type, 'PRECHECK' as capability_code"));
        assertTrue(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'withdrawId' as input_field_id, 'Numero asignado a retiro' as label, "
                        + "'STRING' as field_type, 'PRECHECK' as capability_code"));
        assertTrue(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'amount' as input_field_id, 'Monto' as label, "
                        + "'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, "
                        + "'AMOUNT' as field_group"));
        assertFalse(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, "
                        + "'STRING' as field_type, 'EXECUTE' as capability_code"));
        assertFalse(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'withdrawId' as input_field_id, 'Numero asignado a retiro' as label, "
                        + "'STRING' as field_type, 'EXECUTE' as capability_code"));
        assertFalse(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'amount' as input_field_id, 'Monto' as label, "
                        + "'DOUBLE' as field_type, 'EXECUTE' as capability_code"));
        assertFalse(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, "
                        + "'STRING' as field_type, 'VERIFY' as capability_code"));
        assertFalse(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'withdrawId' as input_field_id, 'Numero asignado a retiro' as label, "
                        + "'STRING' as field_type, 'VERIFY' as capability_code"));
        assertFalse(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'authorization' as input_field_id, "
                        + "'Numero de transaccion original' as label"));
        assertFalse(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, "
                        + "'STRING' as field_type, 'REVERSE' as capability_code"));
        assertFalse(inputFieldsSql.contains(
                "'100708848' as rms_item_code, 'motivo' as input_field_id, 'Motivo del reverso' as label"));
    }
}

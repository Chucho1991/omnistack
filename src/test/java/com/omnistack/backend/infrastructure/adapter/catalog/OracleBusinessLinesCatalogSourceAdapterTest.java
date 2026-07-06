package com.omnistack.backend.infrastructure.adapter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void shouldAssembleCatalogSnapshotFromMultiSourceOracle() {
        NamedParameterJdbcTemplate prodTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        NamedParameterJdbcTemplate rmsTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        OracleBusinessLinesSqlProvider sqlProvider = Mockito.mock(OracleBusinessLinesSqlProvider.class);
        AppProperties appProperties = new AppProperties();

        OracleBusinessLinesCatalogSourceAdapter adapter = new OracleBusinessLinesCatalogSourceAdapter(
                prodTemplate, rmsTemplate, sqlProvider, appProperties);

        BusinessLinesRequest request = BusinessLinesRequest.builder()
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .build();

        when(sqlProvider.getAdServicesSql()).thenReturn("ad-services");
        when(sqlProvider.getRmsItemsSql()).thenReturn("rms-items");
        when(sqlProvider.getRmsSuppliersSql()).thenReturn("rms-suppliers");
        when(sqlProvider.getAdPaymentMethodsSql()).thenReturn("ad-pm");
        when(sqlProvider.getAdCapabilitiesSql()).thenReturn("omni-cap");
        when(sqlProvider.getAdMovementTypesSql()).thenReturn("movement-types");
        when(sqlProvider.getInputFieldsSql()).thenReturn("input-fields");

        // AD: parametros de servicio (via gpf_omnistack. — rmsTemplate)
        when(rmsTemplate.query(eq("ad-services"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.AdServiceRow(
                        "1", "100713841", true, false, "RECA", false,
                        "1", "200", "10000", "3", "3", true, "<html>consent</html>", false)));

        // RMS: metadata del item (CLASS/SUBCLASS, desc)
        when(rmsTemplate.query(eq("rms-items"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.RmsItemRow(
                        "100713841", "1", "ENTRETENIMIENTO", "1", "APUESTAS", "ECUABET CASH IN")));

        // RMS: supplier (nombre y RUC del proveedor)
        when(rmsTemplate.query(eq("rms-suppliers"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.RmsSupplierRow(
                        "100713841", "1", "ECUABET", "9999999999001")));

        // AD: formas de pago (via gpf_omnistack. — rmsTemplate)
        when(rmsTemplate.query(eq("ad-pm"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.AdPaymentMethodRow(
                        "1", "100713841", 2, "TARJETA_CREDITO", true)));

        // PROD (TUKUNAFUNC): capabilities por service_provider_code
        when(prodTemplate.query(eq("omni-cap"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.OmniCapabilityRow("1", "PRECHECK"),
                new OracleBusinessLinesCatalogSourceAdapter.OmniCapabilityRow("1", "CREATE_TICKET")));

        // PROD (TUKUNAFUNC): movement_type por rms_item_code
        when(prodTemplate.query(eq("movement-types"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(
                new OracleBusinessLinesCatalogSourceAdapter.MovementTypeRow("100713841", "CASH_IN")));

        // AD: campos de entrada (stub vacio — via rmsTemplate)
        when(rmsTemplate.query(eq("input-fields"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());

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
        assertTrue(snapshot.getServices().get(0).getInputFields().isEmpty());
        assertEquals("TARJETA_CREDITO", snapshot.getServices().get(0).getPaymentMethods().get(0).getPaymentMethodCode().name());
        assertTrue(snapshot.getServices().get(0).isRequiresConsent());
    }
}

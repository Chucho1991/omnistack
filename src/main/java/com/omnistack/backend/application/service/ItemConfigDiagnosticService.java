package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.AdServicioParametrosSection;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.InputFieldEntry;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.ProviderConfigEntry;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.ProviderWsDefsEntry;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.ProviderWsEntry;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.RmsItemMasterSection;
import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse.RmsItemSupplierSection;
import com.omnistack.backend.infrastructure.adapter.oracle.OracleItemConfigDiagnosticAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio que orquesta la consulta de parametrizacion completa de un item
 * y genera un diagnostico automatico de campos faltantes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemConfigDiagnosticService {

    private final OracleItemConfigDiagnosticAdapter diagnosticAdapter;

    /**
     * Consulta toda la parametrizacion de un rms_item_code en todas las tablas relevantes
     * y genera un diagnostico automatico.
     *
     * @param rmsItemCode codigo del item a diagnosticar
     * @return respuesta completa con datos por tabla y lista de problemas detectados
     */
    public ItemConfigDiagnosticResponse diagnose(String rmsItemCode) {
        log.info("[ItemDiag] Iniciando diagnostico para rms_item_code={}", rmsItemCode);

        // 1) AD_SERVICIO_PARAMETROS
        AdServicioParametrosSection adSection = diagnosticAdapter.queryAdServicioParametros(rmsItemCode);

        // 2) RMS ITEM_MASTER
        RmsItemMasterSection rmsItemMaster = diagnosticAdapter.queryRmsItemMaster(rmsItemCode);

        // 3) RMS ITEM_SUPPLIER
        RmsItemSupplierSection rmsItemSupplier = diagnosticAdapter.queryRmsItemSupplier(rmsItemCode);

        // Resolver service_provider_code para buscar en tablas TUKUNAFUNC
        String serviceProviderCode = adSection.isEncontrado() ? adSection.getServiceProviderCode() : null;

        // 4) IN_OMNI_PROVEEDOR_CONFIG
        List<ProviderConfigEntry> providerConfig = diagnosticAdapter.queryProviderConfig(serviceProviderCode);

        // 5) IN_OMNI_PROVEEDOR_WS
        List<ProviderWsEntry> providerWs = diagnosticAdapter.queryProviderWs(serviceProviderCode, rmsItemCode);

        // 6) IN_OMNI_PROVEEDOR_WS_DEFS (filtrado por item)
        List<ProviderWsDefsEntry> providerWsDefs = diagnosticAdapter.queryProviderWsDefs(serviceProviderCode, rmsItemCode);

        // 7) IN_OMNI_INPUT_FIELDS
        List<InputFieldEntry> inputFields = diagnosticAdapter.queryInputFields(rmsItemCode);

        // 8) Diagnostico automatico
        List<String> diagnostico = buildDiagnostico(
                rmsItemCode, adSection, rmsItemMaster, rmsItemSupplier,
                providerConfig, providerWs, providerWsDefs, inputFields);

        log.info("[ItemDiag] Diagnostico completado para rms_item_code={}, problemas={}", rmsItemCode, diagnostico.size());

        return ItemConfigDiagnosticResponse.builder()
                .rmsItemCode(rmsItemCode)
                .adServicioParametros(adSection)
                .rmsItemMaster(rmsItemMaster)
                .rmsItemSupplier(rmsItemSupplier)
                .providerConfig(providerConfig)
                .providerWs(providerWs)
                .providerWsDefs(providerWsDefs)
                .inputFields(inputFields)
                .diagnostico(diagnostico)
                .build();
    }

    private List<String> buildDiagnostico(
            String rmsItemCode,
            AdServicioParametrosSection adSection,
            RmsItemMasterSection rmsItemMaster,
            RmsItemSupplierSection rmsItemSupplier,
            List<ProviderConfigEntry> providerConfig,
            List<ProviderWsEntry> providerWs,
            List<ProviderWsDefsEntry> providerWsDefs,
            List<InputFieldEntry> inputFields) {

        List<String> problemas = new ArrayList<>();

        // --- Validar AD_SERVICIO_PARAMETROS ---
        if (!adSection.isEncontrado()) {
            problemas.add("[AD_SERVICIO_PARAMETROS] Item " + rmsItemCode + " NO existe en AD_SERVICIO_PARAMETROS — no aparecera en el catalogo.");
            return problemas; // Sin esto nada mas funciona
        }
        if (adSection.getActivoEnCanal() == null || !adSection.getActivoEnCanal()) {
            problemas.add("[AD_CANAL_SERVICIO] Item " + rmsItemCode + " NO tiene entrada activa en AD_CANAL_SERVICIO para canal_codigo=1 (POS).");
        }

        // --- Validar ITEM_MASTER ---
        if (!rmsItemMaster.isEncontrado()) {
            problemas.add("[ITEM_MASTER] Item " + rmsItemCode + " NO existe en ITEM_MASTER — no se puede resolver category/subcategory.");
        }

        // --- Validar ITEM_SUPPLIER ---
        if (!rmsItemSupplier.isEncontrado()) {
            problemas.add("[ITEM_SUPPLIER] Item " + rmsItemCode + " NO existe en ITEM_SUPPLIER — no se puede resolver proveedor RMS.");
        }

        // --- Validar IN_OMNI_PROVEEDOR_CONFIG ---
        if (providerConfig.isEmpty()) {
            problemas.add("[IN_OMNI_PROVEEDOR_CONFIG] No existe proveedor registrado con service_provider_code=" + adSection.getServiceProviderCode() + ".");
        } else {
            Set<String> configKeys = providerConfig.stream()
                    .map(ProviderConfigEntry::getConfigKey).collect(Collectors.toSet());
            if (!configKeys.contains("service_provider_code")) {
                problemas.add("[IN_OMNI_PROVEEDOR_CONFIG] Falta config_key='service_provider_code'.");
            }
        }

        // --- Validar IN_OMNI_PROVEEDOR_WS (operaciones con item configurado) ---
        // Detectar movement_type del item por las operaciones que tiene en WS_DEFS
        boolean isCashOut = providerWsDefs.stream()
                .anyMatch(d -> d.getWsKey() != null && d.getWsKey().toUpperCase().contains("CASHOUT"));
        boolean isCashIn = providerWsDefs.stream()
                .anyMatch(d -> d.getWsKey() != null && d.getWsKey().toUpperCase().contains("CASHIN"));

        if (providerWs.isEmpty() && providerWsDefs.isEmpty()) {
            problemas.add("[IN_OMNI_PROVEEDOR_WS_DEFS] Item " + rmsItemCode + " NO tiene ninguna entrada en WS_DEFS — hasConfiguredOperation() fallara para todas las capabilities.");
        } else {
            Set<String> wsKeysWithItem = providerWs.stream()
                    .filter(ws -> "S".equalsIgnoreCase(ws.getEnabled()))
                    .map(ProviderWsEntry::getWsKey)
                    .collect(Collectors.toSet());

            String suffix = isCashOut ? ".CASHOUT" : (isCashIn ? ".CASHIN" : null);
            if (suffix != null) {
                List<String> expectedOps = List.of("PRECHECK" + suffix, "EXECUTE" + suffix, "VERIFY" + suffix, "REVERSE" + suffix);
                for (String op : expectedOps) {
                    if (!wsKeysWithItem.contains(op)) {
                        problemas.add("[IN_OMNI_PROVEEDOR_WS_DEFS] Falta entrada 'item." + rmsItemCode
                                + "' para WS_KEY='" + op + "' — hasConfiguredOperation() retornara false para esta capability.");
                    }
                }
            }
        }

        // --- Validar IN_OMNI_INPUT_FIELDS ---
        if (inputFields.isEmpty()) {
            problemas.add("[IN_OMNI_INPUT_FIELDS] Item " + rmsItemCode + " NO tiene campos de entrada configurados — el front no sabra que campos pedir.");
        }

        if (problemas.isEmpty()) {
            problemas.add("OK — No se detectaron problemas de parametrizacion.");
        }

        return problemas;
    }
}

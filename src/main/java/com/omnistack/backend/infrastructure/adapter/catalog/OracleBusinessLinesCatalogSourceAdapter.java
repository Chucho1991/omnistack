package com.omnistack.backend.infrastructure.adapter.catalog;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.port.out.CatalogSourcePort;
import com.omnistack.backend.application.port.out.BusinessLinesCatalogSourcePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.FlgItem;
import com.omnistack.backend.domain.enums.InputFieldType;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.enums.PaymentMethodCode;
import com.omnistack.backend.domain.model.CatalogSnapshot;
import com.omnistack.backend.domain.model.Category;
import com.omnistack.backend.domain.model.CollectionSubcategory;
import com.omnistack.backend.domain.model.InputField;
import com.omnistack.backend.domain.model.PaymentMethod;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.domain.model.ServiceProvider;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.business-lines", name = "source", havingValue = "oracle", matchIfMissing = true)
public class OracleBusinessLinesCatalogSourceAdapter implements BusinessLinesCatalogSourcePort, CatalogSourcePort {

    private final NamedParameterJdbcTemplate prodJdbcTemplate;
    private final NamedParameterJdbcTemplate rmsJdbcTemplate;
    private final OracleBusinessLinesSqlProvider sqlProvider;
    private final AppProperties appProperties;

    public OracleBusinessLinesCatalogSourceAdapter(
            @Qualifier("prodOracleJdbcTemplate") NamedParameterJdbcTemplate prodJdbcTemplate,
            @Qualifier("rmsOracleJdbcTemplate") NamedParameterJdbcTemplate rmsJdbcTemplate,
            OracleBusinessLinesSqlProvider sqlProvider,
            AppProperties appProperties) {
        this.prodJdbcTemplate = prodJdbcTemplate;
        this.rmsJdbcTemplate = rmsJdbcTemplate;
        this.sqlProvider = sqlProvider;
        this.appProperties = appProperties;
    }

    @Override
    public CatalogSnapshot loadCatalogSnapshot() {
        AppProperties.BusinessLines.DefaultRequest request = appProperties.getBusinessLines().getDefaultRequest();
        return loadCatalogSnapshot(BusinessLinesRequest.builder()
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(ChannelPos.valueOf(request.getChannelPos()))
                .build());
    }

    @Override
    public CatalogSnapshot loadCatalogSnapshot(BusinessLinesRequest request) {
        int canalCodigo = appProperties.getBusinessLines().getCanalCodigos()
                .getOrDefault(Objects.requireNonNull(request.getChannelPos()).name(), 1);
        log.debug("[BL-catalog] START channelPos={} → canal_codigo={}", request.getChannelPos(), canalCodigo);

        MapSqlParameterSource adParams = new MapSqlParameterSource()
                .addValue("canal_codigo", canalCodigo);

        // --- AD: parametros de servicio por item activo en el canal (via gpf_omnistack.) ---
        List<AdServiceRow> adServices = rmsJdbcTemplate.query(
                sqlProvider.getAdServicesSql(), adParams, adServiceRowMapper());
        log.debug("[BL-catalog] adServices={}", adServices.size());
        if (adServices.isEmpty()) {
            log.warn("[BL-catalog] adServices vacío para canal_codigo={} — retornando snapshot vacío", canalCodigo);
            return emptySnapshot();
        }

        Set<String> activeItemCodes = adServices.stream()
                .map(AdServiceRow::rmsItemCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        log.debug("[BL-catalog] activeItemCodes={} → {}", activeItemCodes.size(), activeItemCodes);

        // --- RMS: metadata de items (CLASS, SUBCLASS, desc, tipo movimiento) ---
        MapSqlParameterSource rmsParams = new MapSqlParameterSource(
                "rms_item_codes", new ArrayList<>(activeItemCodes));
        List<RmsItemRow> rmsItems = rmsJdbcTemplate.query(
                sqlProvider.getRmsItemsSql(), rmsParams, rmsItemRowMapper());
        log.debug("[BL-catalog] rmsItems={}", rmsItems.size());
        if (rmsItems.isEmpty()) {
            log.warn("[BL-catalog] rmsItems vacío para items={} — no se podrán construir categorías", activeItemCodes);
        }

        List<RmsSupplierRow> rmsSuppliers = rmsJdbcTemplate.query(
                sqlProvider.getRmsSuppliersSql(), rmsParams, rmsSupplierRowMapper());
        log.debug("[BL-catalog] rmsSuppliers={}", rmsSuppliers.size());

        // --- AD: formas de pago por item (via gpf_omnistack.) ---
        List<AdPaymentMethodRow> adPaymentMethods = rmsJdbcTemplate.query(
                sqlProvider.getAdPaymentMethodsSql(), adParams, adPaymentMethodRowMapper());
        log.debug("[BL-catalog] adPaymentMethods={}", adPaymentMethods.size());

        // --- PROD (TUKUNAFUNC): capabilities por service_provider_code (IN_OMNI_PROVEEDOR_WS) ---
        List<OmniCapabilityRow> omniCapabilities = prodJdbcTemplate.query(
                sqlProvider.getAdCapabilitiesSql(), new MapSqlParameterSource(), omniCapabilityRowMapper());
        log.debug("[BL-catalog] omniCapabilities={}", omniCapabilities.size());
        if (omniCapabilities.isEmpty()) {
            log.warn("[BL-catalog] omniCapabilities vacío — los servicios quedarán sin capabilities y serán filtrados");
        }

        // --- PROD (TUKUNAFUNC): movement_type por rms_item_code (IN_OMNI_PROVEEDOR_WS_DEFS) ---
        List<MovementTypeRow> movementTypeRows = prodJdbcTemplate.query(
                sqlProvider.getAdMovementTypesSql(), new MapSqlParameterSource(), movementTypeRowMapper());
        log.debug("[BL-catalog] movementTypeRows={}", movementTypeRows.size());
        Map<String, String> movementTypeByItem = movementTypeRows.stream()
                .collect(Collectors.toMap(MovementTypeRow::rmsItemCode, MovementTypeRow::movementType,
                        (a, b) -> a, LinkedHashMap::new));

        // --- PROD (TUKUNAFUNC): campos de entrada por item (IN_OMNI_INPUT_FIELDS) ---
        List<InputFieldRow> inputFieldRows = prodJdbcTemplate.query(
                sqlProvider.getInputFieldsSql(), rmsParams, inputFieldRowMapper());
        log.debug("[BL-catalog] inputFieldRows={}", inputFieldRows.size());

        // --- Mapas de lookup ---
        Map<String, RmsItemRow> rmsItemMap = rmsItems.stream()
                .collect(Collectors.toMap(RmsItemRow::rmsItemCode, r -> r, (a, b) -> a, LinkedHashMap::new));
        Map<String, RmsSupplierRow> rmsSupplierByItem = rmsSuppliers.stream()
                .collect(Collectors.toMap(RmsSupplierRow::rmsItemCode, r -> r, (a, b) -> a, LinkedHashMap::new));
        Map<String, List<String>> capabilityCodesByProvider = omniCapabilities.stream()
                .collect(Collectors.groupingBy(OmniCapabilityRow::serviceProviderCode,
                        LinkedHashMap::new,
                        Collectors.mapping(OmniCapabilityRow::capabilityCode, Collectors.toList())));
        log.info("[BL-catalog][DIAG] capabilityCodesByProvider keys={}", capabilityCodesByProvider.keySet());
        log.info("[BL-catalog][DIAG] activeItemCodes={}", activeItemCodes);

        // --- Construir CategorySubcategoryRow: CLASS/SUBCLASS distintos desde RMS ---
        List<CategorySubcategoryRow> categoryRows = rmsItems.stream()
                .filter(r -> activeItemCodes.contains(r.rmsItemCode()))
                .collect(Collectors.toMap(
                        r -> new SubcategoryKey(r.categoryCode(), r.subcategoryCode()),
                        r -> new CategorySubcategoryRow(r.categoryCode(), r.categoryName(),
                                r.subcategoryCode(), r.subcategoryName(), true),
                        (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();
        log.debug("[BL-catalog] categoryRows={}", categoryRows.size());

        // --- Construir ServiceProviderRow: distintivo por (cat, subcat, provider) ---
        long adServicesWithRmsMatch = adServices.stream().filter(r -> rmsItemMap.containsKey(r.rmsItemCode())).count();
        log.debug("[BL-catalog] adServices con match en rmsItemMap={}/{}", adServicesWithRmsMatch, adServices.size());
        List<ServiceProviderRow> providerRows = adServices.stream()
                .filter(r -> rmsItemMap.containsKey(r.rmsItemCode()))
                .map(r -> {
                    RmsItemRow rms = rmsItemMap.get(r.rmsItemCode());
                    RmsSupplierRow sup = rmsSupplierByItem.get(r.rmsItemCode());
                    String provName = sup != null ? sup.providerName() : r.serviceProviderCode();
                    String rucProv = sup != null ? sup.rucProvider() : "";
                    return new ServiceProviderRow(
                            rms.categoryCode(), rms.subcategoryCode(),
                            r.serviceProviderCode(), rucProv, provName, true);
                })
                .collect(Collectors.toMap(
                        ServiceProviderRow::providerKey,
                        r -> r,
                        (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();
        log.debug("[BL-catalog] providerRows={}", providerRows.size());

        // --- Construir ServiceRow: campos AD + metadata RMS + movement_type de TUKUNAFUNC ---
        List<ServiceRow> serviceRows = adServices.stream()
                .filter(r -> rmsItemMap.containsKey(r.rmsItemCode()))
                .map(r -> {
                    RmsItemRow rms = rmsItemMap.get(r.rmsItemCode());
                    String movementType = movementTypeByItem.getOrDefault(r.rmsItemCode(), "CASH_IN");
                    return new ServiceRow(
                            rms.categoryCode(), rms.subcategoryCode(),
                            r.serviceProviderCode(), r.rmsItemCode(),
                            rms.description(), r.active(),
                            null,
                            movementType,
                            r.mixedPayment(), r.flgItem(), r.refund(),
                            r.minAmount(), r.maxAmount(),
                            r.timeoutWsMax(), r.retriesWsMax(), r.numTickets(),
                            r.requiresConsent(), r.consentText());
                })
                .toList();
        log.debug("[BL-catalog] serviceRows={}", serviceRows.size());

        // --- Construir CapabilityRow: expandir capabilities por item segun provider ---
        List<CapabilityRow> capabilityRows = serviceRows.stream()
                .flatMap(service -> {
                    List<String> caps = capabilityCodesByProvider.getOrDefault(service.serviceProviderCode(), List.of());
                    if (caps.isEmpty()) {
                        log.info("[BL-catalog][DIAG] sin caps — serviceProviderCode='{}' (len={}) rmsItemCode='{}'",
                                service.serviceProviderCode(), service.serviceProviderCode() == null ? -1 : service.serviceProviderCode().length(), service.rmsItemCode());
                    }
                    return caps.stream()
                            .map(cap -> new CapabilityRow(
                                    service.categoryCode(), service.subcategoryCode(),
                                    service.serviceProviderCode(), service.rmsItemCode(), cap));
                })
                .toList();
        log.info("[BL-catalog][DIAG] capabilityRows={} serviceRows={}", capabilityRows.size(), serviceRows.size());
        serviceRows.forEach(s -> log.info(
                "[BL-catalog][DIAG] serviceRow: cat='{}' subcat='{}' spc='{}' item='{}' movType='{}'",
                s.categoryCode(), s.subcategoryCode(), s.serviceProviderCode(), s.rmsItemCode(), s.movementType()));

        // --- Construir PaymentMethodRow: formas de pago AD + categoria/subcategoria RMS ---
        List<PaymentMethodRow> paymentMethodRows = adPaymentMethods.stream()
                .filter(r -> rmsItemMap.containsKey(r.rmsItemCode()))
                .map(r -> {
                    RmsItemRow rms = rmsItemMap.get(r.rmsItemCode());
                    return new PaymentMethodRow(
                            rms.categoryCode(), rms.subcategoryCode(),
                            r.serviceProviderCode(), r.rmsItemCode(),
                            r.servicePaymentMethodId(), r.paymentMethodCode(), r.active());
                })
                .toList();

        // === Ensamblado final (igual que antes) ===
        Map<ServiceKey, List<Capability>> capabilitiesByService = capabilityRows.stream()
                .collect(Collectors.groupingBy(
                        CapabilityRow::serviceKey,
                        LinkedHashMap::new,
                        Collectors.mapping(row -> Capability.valueOf(row.capabilityCode()), Collectors.toList())));

        Map<ServiceKey, List<InputField>> inputFieldsByService = inputFieldRows.stream()
                .collect(Collectors.groupingBy(
                        InputFieldRow::serviceKey,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toInputField, Collectors.toList())));

        Map<ServiceKey, List<PaymentMethod>> paymentMethodsByService = paymentMethodRows.stream()
                .collect(Collectors.groupingBy(
                        PaymentMethodRow::serviceKey,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.mapping(this::toPaymentMethod, Collectors.toList()),
                                list -> list.stream().filter(Objects::nonNull).toList())));

        List<ServiceDefinition> services = serviceRows.stream()
                .map(row -> toServiceDefinition(
                        row,
                        capabilitiesByService.getOrDefault(row.serviceKey(), List.of()),
                        inputFieldsByService.getOrDefault(row.serviceKey(), List.of()),
                        paymentMethodsByService.getOrDefault(row.serviceKey(), List.of())))
                .toList();

        Map<ProviderKey, List<ServiceDefinition>> servicesByProvider = services.stream()
                .collect(Collectors.groupingBy(
                        service -> new ProviderKey(service.getCategoryCode(), service.getSubcategoryCode(), service.getServiceProviderCode()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<SubcategoryKey, List<ServiceProvider>> providersBySubcategory = providerRows.stream()
                .collect(Collectors.groupingBy(
                        ServiceProviderRow::subcategoryKey,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                row -> ServiceProvider.builder()
                                        .serviceProviderCode(row.serviceProviderCode())
                                        .rucProvider(row.rucProvider())
                                        .providerName(row.providerName())
                                        .active(row.active())
                                        .services(servicesByProvider.getOrDefault(row.providerKey(), List.of()))
                                        .build(),
                                Collectors.toList())));

        Map<String, CategoryAccumulator> categories = new LinkedHashMap<>();
        for (CategorySubcategoryRow row : categoryRows) {
            CategoryAccumulator categoryAccumulator = categories.computeIfAbsent(
                    row.categoryCode(),
                    key -> new CategoryAccumulator(row.categoryCode(), row.categoryName()));
            categoryAccumulator.subcategories().add(CollectionSubcategory.builder()
                    .subcategoryCode(row.subcategoryCode())
                    .subcategoryName(row.subcategoryName())
                    .active(row.active())
                    .providers(providersBySubcategory.getOrDefault(row.subcategoryKey(), List.of()))
                    .build());
        }

        List<Category> finalCategories = categories.values().stream()
                .map(accumulator -> Category.builder()
                        .categoryCode(accumulator.categoryCode())
                        .categoryName(accumulator.categoryName())
                        .subcategories(List.copyOf(accumulator.subcategories()))
                        .build())
                .toList();

        log.debug("[BL-catalog] RESULT categories={} subcategories={} services={} providers={}",
                finalCategories.size(),
                finalCategories.stream().mapToLong(c -> c.getSubcategories().size()).sum(),
                services.size(),
                finalCategories.stream()
                        .flatMap(c -> c.getSubcategories().stream())
                        .mapToLong(s -> s.getProviders().size()).sum());

        return CatalogSnapshot.builder()
                .categories(finalCategories)
                .services(services)
                .loadedAt(OffsetDateTime.now())
                .version("oracle-multi-source-v2")
                .build();
    }

    private CatalogSnapshot emptySnapshot() {
        return CatalogSnapshot.builder()
                .categories(List.of())
                .services(List.of())
                .loadedAt(OffsetDateTime.now())
                .version("oracle-multi-source-v2")
                .build();
    }

    private ServiceDefinition toServiceDefinition(
            ServiceRow row,
            List<Capability> capabilities,
            List<InputField> inputFields,
            List<PaymentMethod> paymentMethods) {
        return ServiceDefinition.builder()
                .categoryCode(row.categoryCode())
                .subcategoryCode(row.subcategoryCode())
                .serviceProviderCode(row.serviceProviderCode())
                .rmsItemCode(row.rmsItemCode())
                .description(row.description())
                .active(row.active())
                .jdeCode(row.jdeCode())
                .movementType(MovementType.valueOf(row.movementType().replace(' ', '_').toUpperCase(Locale.ROOT)))
                .mixedPayment(row.mixedPayment())
                .flgItem(FlgItem.valueOf(row.flgItem()))
                .refund(row.refund())
                .minAmount(new BigDecimal(row.minAmount()))
                .maxAmount(new BigDecimal(row.maxAmount()))
                .timeoutWsMax(row.timeoutWsMax())
                .retriesWsMax(row.retriesWsMax())
                .numTickets(row.numTickets())
                .capabilities(capabilities)
                .inputFields(inputFields)
                .paymentMethods(paymentMethods)
                .requiresConsent(row.requiresConsent())
                .consentText(row.consentText())
                .build();
    }

    private InputField toInputField(InputFieldRow row) {
        return InputField.builder()
                .id(row.inputFieldId())
                .label(row.label())
                .type(InputFieldType.valueOf(row.fieldType()))
                .capability(row.capabilityCode())
                .required(row.required())
                .group(row.fieldGroup())
                .conditional(row.conditionalOperator())
                .build();
    }

    private PaymentMethod toPaymentMethod(PaymentMethodRow row) {
        PaymentMethodCode code;
        try {
            code = PaymentMethodCode.valueOf(row.paymentMethodCode().replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        return PaymentMethod.builder()
                .servicePaymentMethodId(row.servicePaymentMethodId())
                .paymentMethodCode(code)
                .active(row.active())
                .description(row.paymentMethodCode())
                .build();
    }

    private static String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toPlainString() : "0";
    }

    // ---- Row mappers ----

    private RowMapper<AdServiceRow> adServiceRowMapper() {
        return (rs, rowNum) -> new AdServiceRow(
                rs.getString("service_provider_code"),
                rs.getString("rms_item_code"),
                rs.getInt("is_active") == 1,
                rs.getInt("is_mixed_payment") == 1,
                rs.getString("flg_item"),
                rs.getInt("is_refund") == 1,
                bigDecimalToString(rs.getBigDecimal("min_amount")),
                bigDecimalToString(rs.getBigDecimal("max_amount")),
                rs.getString("timeout_ws_max"),
                rs.getString("retries_ws_max"),
                rs.getString("num_tickets"),
                rs.getInt("requires_consent") == 1,
                rs.getString("consent_text"));
    }

    private RowMapper<MovementTypeRow> movementTypeRowMapper() {
        return (rs, rowNum) -> new MovementTypeRow(
                rs.getString("rms_item_code"),
                rs.getString("movement_type"));
    }

    private RowMapper<AdPaymentMethodRow> adPaymentMethodRowMapper() {
        return (rs, rowNum) -> new AdPaymentMethodRow(
                rs.getString("service_provider_code"),
                rs.getString("rms_item_code"),
                rs.getInt("service_payment_method_id"),
                rs.getString("payment_method_code"),
                rs.getInt("is_active") == 1);
    }

    private RowMapper<OmniCapabilityRow> omniCapabilityRowMapper() {
        return (rs, rowNum) -> new OmniCapabilityRow(
                rs.getString("service_provider_code"),
                rs.getString("capability_code"));
    }

    private RowMapper<RmsItemRow> rmsItemRowMapper() {
        return (rs, rowNum) -> new RmsItemRow(
                rs.getString("rms_item_code"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("subcategory_code"),
                rs.getString("subcategory_name"),
                rs.getString("description"));
    }

    private RowMapper<RmsSupplierRow> rmsSupplierRowMapper() {
        return (rs, rowNum) -> new RmsSupplierRow(
                rs.getString("rms_item_code"),
                rs.getString("supplier_code"),
                rs.getString("provider_name"),
                rs.getString("ruc_provider"));
    }

    private RowMapper<InputFieldRow> inputFieldRowMapper() {
        return (rs, rowNum) -> new InputFieldRow(
                rs.getString("category_code"),
                rs.getString("subcategory_code"),
                rs.getString("service_provider_code"),
                rs.getString("rms_item_code"),
                rs.getString("input_field_id"),
                rs.getString("label"),
                rs.getString("field_type"),
                rs.getString("capability_code"),
                rs.getInt("is_required") == 1,
                rs.getString("field_group"),
                rs.getString("conditional_operator"));
    }

    // ---- Intermediate row records (multi-source queries) ----

    record AdServiceRow(
            String serviceProviderCode,
            String rmsItemCode,
            boolean active,
            boolean mixedPayment,
            String flgItem,
            boolean refund,
            String minAmount,
            String maxAmount,
            String timeoutWsMax,
            String retriesWsMax,
            String numTickets,
            boolean requiresConsent,
            String consentText) {}

    record MovementTypeRow(String rmsItemCode, String movementType) {}

    record AdPaymentMethodRow(
            String serviceProviderCode,
            String rmsItemCode,
            int servicePaymentMethodId,
            String paymentMethodCode,
            boolean active) {}

    record OmniCapabilityRow(String serviceProviderCode, String capabilityCode) {}

    record RmsItemRow(
            String rmsItemCode,
            String categoryCode,
            String categoryName,
            String subcategoryCode,
            String subcategoryName,
            String description) {}

    record RmsSupplierRow(
            String rmsItemCode,
            String supplierCode,
            String providerName,
            String rucProvider) {}

    // ---- Assembly row records (mismos que antes — usados por la logica de ensamblado) ----

    record CategoryAccumulator(String categoryCode, String categoryName, List<CollectionSubcategory> subcategories) {
        CategoryAccumulator(String categoryCode, String categoryName) {
            this(categoryCode, categoryName, new ArrayList<>());
        }
    }

    record CategorySubcategoryRow(
            String categoryCode,
            String categoryName,
            String subcategoryCode,
            String subcategoryName,
            boolean active) {
        SubcategoryKey subcategoryKey() {
            return new SubcategoryKey(categoryCode, subcategoryCode);
        }
    }

    record ServiceProviderRow(
            String categoryCode,
            String subcategoryCode,
            String serviceProviderCode,
            String rucProvider,
            String providerName,
            boolean active) {
        SubcategoryKey subcategoryKey() {
            return new SubcategoryKey(categoryCode, subcategoryCode);
        }

        ProviderKey providerKey() {
            return new ProviderKey(categoryCode, subcategoryCode, serviceProviderCode);
        }
    }

    record ServiceRow(
            String categoryCode,
            String subcategoryCode,
            String serviceProviderCode,
            String rmsItemCode,
            String description,
            boolean active,
            String jdeCode,
            String movementType,
            boolean mixedPayment,
            String flgItem,
            boolean refund,
            String minAmount,
            String maxAmount,
            String timeoutWsMax,
            String retriesWsMax,
            String numTickets,
            boolean requiresConsent,
            String consentText) {
        ServiceKey serviceKey() {
            return new ServiceKey(categoryCode, subcategoryCode, serviceProviderCode, rmsItemCode);
        }
    }

    record CapabilityRow(
            String categoryCode,
            String subcategoryCode,
            String serviceProviderCode,
            String rmsItemCode,
            String capabilityCode) {
        ServiceKey serviceKey() {
            return new ServiceKey(categoryCode, subcategoryCode, serviceProviderCode, rmsItemCode);
        }
    }

    record InputFieldRow(
            String categoryCode,
            String subcategoryCode,
            String serviceProviderCode,
            String rmsItemCode,
            String inputFieldId,
            String label,
            String fieldType,
            String capabilityCode,
            boolean required,
            String fieldGroup,
            String conditionalOperator) {
        ServiceKey serviceKey() {
            return new ServiceKey(categoryCode, subcategoryCode, serviceProviderCode, rmsItemCode);
        }
    }

    record PaymentMethodRow(
            String categoryCode,
            String subcategoryCode,
            String serviceProviderCode,
            String rmsItemCode,
            Integer servicePaymentMethodId,
            String paymentMethodCode,
            boolean active) {
        ServiceKey serviceKey() {
            return new ServiceKey(categoryCode, subcategoryCode, serviceProviderCode, rmsItemCode);
        }
    }

    record SubcategoryKey(String categoryCode, String subcategoryCode) {}

    record ProviderKey(String categoryCode, String subcategoryCode, String serviceProviderCode) {}

    record ServiceKey(String categoryCode, String subcategoryCode, String serviceProviderCode, String rmsItemCode) {}
}

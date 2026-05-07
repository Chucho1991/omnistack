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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter Oracle para el catalogo del endpoint business-lines.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.business-lines", name = "source", havingValue = "oracle", matchIfMissing = true)
public class OracleBusinessLinesCatalogSourceAdapter implements BusinessLinesCatalogSourcePort, CatalogSourcePort {

    @Qualifier("businessLinesOracleNamedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OracleBusinessLinesSqlProvider sqlProvider;
    private final AppProperties appProperties;

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
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chain", request.getChain())
                .addValue("store", request.getStore())
                .addValue("store_name", request.getStoreName())
                .addValue("pos", request.getPos())
                .addValue("channel_pos", Objects.requireNonNull(request.getChannelPos()).name());

        List<CategorySubcategoryRow> categoryRows = jdbcTemplate.query(
                sqlProvider.getCategorySubcategorySql(),
                params,
                categorySubcategoryRowMapper());
        List<ServiceProviderRow> providerRows = jdbcTemplate.query(
                sqlProvider.getServiceProvidersSql(),
                params,
                serviceProviderRowMapper());
        List<ServiceRow> serviceRows = jdbcTemplate.query(
                sqlProvider.getServicesSql(),
                params,
                serviceRowMapper());
        List<CapabilityRow> capabilityRows = jdbcTemplate.query(
                sqlProvider.getCapabilitiesSql(),
                params,
                capabilityRowMapper());
        List<InputFieldRow> inputFieldRows = jdbcTemplate.query(
                sqlProvider.getInputFieldsSql(),
                params,
                inputFieldRowMapper());
        List<PaymentMethodRow> paymentMethodRows = jdbcTemplate.query(
                sqlProvider.getPaymentMethodsSql(),
                params,
                paymentMethodRowMapper());

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
                        Collectors.mapping(this::toPaymentMethod, Collectors.toList())));

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

        return CatalogSnapshot.builder()
                .categories(categories.values().stream()
                        .map(accumulator -> Category.builder()
                                .categoryCode(accumulator.categoryCode())
                                .categoryName(accumulator.categoryName())
                                .subcategories(List.copyOf(accumulator.subcategories()))
                                .build())
                        .toList())
                .services(services)
                .loadedAt(OffsetDateTime.now())
                .version("oracle-dual-mock-v1")
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
                .movementType(MovementType.valueOf(row.movementType()))
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
        return PaymentMethod.builder()
                .servicePaymentMethodId(row.servicePaymentMethodId())
                .paymentMethodCode(PaymentMethodCode.valueOf(row.paymentMethodCode().replace(' ', '_').toUpperCase(Locale.ROOT)))
                .active(row.active())
                .description(row.paymentMethodCode())
                .build();
    }

    private RowMapper<CategorySubcategoryRow> categorySubcategoryRowMapper() {
        return (rs, rowNum) -> new CategorySubcategoryRow(
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("subcategory_code"),
                rs.getString("subcategory_name"),
                rs.getInt("is_active") == 1);
    }

    private RowMapper<ServiceProviderRow> serviceProviderRowMapper() {
        return (rs, rowNum) -> new ServiceProviderRow(
                rs.getString("category_code"),
                rs.getString("subcategory_code"),
                rs.getString("service_provider_code"),
                rs.getString("ruc_provider"),
                rs.getString("provider_name"),
                rs.getInt("is_active") == 1);
    }

    private RowMapper<ServiceRow> serviceRowMapper() {
        return (rs, rowNum) -> new ServiceRow(
                rs.getString("category_code"),
                rs.getString("subcategory_code"),
                rs.getString("service_provider_code"),
                rs.getString("rms_item_code"),
                rs.getString("description"),
                rs.getInt("is_active") == 1,
                rs.getString("jde_code"),
                rs.getString("movement_type"),
                rs.getInt("is_mixed_payment") == 1,
                rs.getString("flg_item"),
                rs.getInt("is_refund") == 1,
                rs.getString("min_amount"),
                rs.getString("max_amount"),
                rs.getString("timeout_ws_max"),
                rs.getString("retries_ws_max"),
                rs.getString("num_tickets"),
                rs.getInt("requires_consent") == 1,
                rs.getString("consent_text"));
    }

    private RowMapper<CapabilityRow> capabilityRowMapper() {
        return (rs, rowNum) -> new CapabilityRow(
                rs.getString("category_code"),
                rs.getString("subcategory_code"),
                rs.getString("service_provider_code"),
                rs.getString("rms_item_code"),
                rs.getString("capability_code"));
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

    private RowMapper<PaymentMethodRow> paymentMethodRowMapper() {
        return (rs, rowNum) -> new PaymentMethodRow(
                rs.getString("category_code"),
                rs.getString("subcategory_code"),
                rs.getString("service_provider_code"),
                rs.getString("rms_item_code"),
                rs.getInt("service_payment_method_id"),
                rs.getString("payment_method_code"),
                rs.getInt("is_active") == 1);
    }

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

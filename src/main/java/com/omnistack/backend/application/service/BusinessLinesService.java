package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.BusinessLineCollectionSubcategoryResponse;
import com.omnistack.backend.application.dto.BusinessLineInputFieldResponse;
import com.omnistack.backend.application.dto.BusinessLinePaymentMethodResponse;
import com.omnistack.backend.application.dto.BusinessLineProviderResponse;
import com.omnistack.backend.application.dto.BusinessLineServiceResponse;
import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.dto.BusinessLinesResponse;
import com.omnistack.backend.application.mapper.ResponseFactory;
import com.omnistack.backend.application.port.in.BusinessLinesUseCase;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.CollectionSubcategory;
import com.omnistack.backend.domain.model.InputField;
import com.omnistack.backend.domain.model.PaymentMethod;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.domain.model.ServiceProvider;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para consulta de lineas de negocio.
 */
@Service
@RequiredArgsConstructor
public class BusinessLinesService implements BusinessLinesUseCase {

    private static final String DEFAULT_NUM_TICKETS = "3";
    private static final String PROVIDER_NAME_PLACEHOLDER = "{{provider_name}}";

    private final BusinessLinesCatalogCacheService businessLinesCatalogCacheService;
    private final AppProperties appProperties;

    @Override
    public BusinessLinesResponse getBusinessLines(BusinessLinesRequest request) {
        Map<SubcategoryResponseKey, BusinessLineCollectionSubcategoryResponse> groupedSubcategories =
                businessLinesCatalogCacheService.getCatalogSnapshot(request)
                .getCategories().stream()
                .flatMap(category -> category.getSubcategories().stream()
                        .map(subcategory -> toCollectionSubcategoryResponse(
                                category.getCategoryCode(),
                                category.getCategoryName(),
                                subcategory,
                                request)))
                .filter(subcategory -> !subcategory.getServiceProviders().isEmpty())
                .collect(Collectors.toMap(
                        response -> new SubcategoryResponseKey(
                                response.getCategoryCode(), response.getSubcategoryCode()),
                        response -> response,
                        this::mergeSubcategoryResponses,
                        LinkedHashMap::new));

        return ResponseFactory.businessLines(request, List.copyOf(groupedSubcategories.values()));
    }

    private BusinessLineCollectionSubcategoryResponse toCollectionSubcategoryResponse(
            String categoryCode,
            String categoryName,
            CollectionSubcategory subcategory,
            BusinessLinesRequest request) {
        return BusinessLineCollectionSubcategoryResponse.builder()
                .categoryCode(categoryCode)
                .categoryName(categoryName)
                .subcategoryCode(subcategory.getSubcategoryCode())
                .subcategoryName(subcategory.getSubcategoryName())
                .active(subcategory.isActive())
                .serviceProviders(toProviderResponses(subcategory, request))
                .build();
    }

    private List<BusinessLineProviderResponse> toProviderResponses(
            CollectionSubcategory subcategory,
            BusinessLinesRequest request) {
        Map<String, BusinessLineProviderResponse> providersByCode = subcategory.getProviders().stream()
                .map(provider -> toProviderResponse(provider, request))
                .filter(provider -> !provider.getServices().isEmpty())
                .collect(Collectors.toMap(
                        BusinessLineProviderResponse::getServiceProviderCode,
                        provider -> provider,
                        this::mergeProviderResponses,
                        LinkedHashMap::new));
        return List.copyOf(providersByCode.values());
    }

    private BusinessLineCollectionSubcategoryResponse mergeSubcategoryResponses(
            BusinessLineCollectionSubcategoryResponse first,
            BusinessLineCollectionSubcategoryResponse second) {
        return BusinessLineCollectionSubcategoryResponse.builder()
                .categoryCode(first.getCategoryCode())
                .categoryName(first.getCategoryName())
                .subcategoryCode(first.getSubcategoryCode())
                .subcategoryName(first.getSubcategoryName())
                .active(first.isActive() || second.isActive())
                .serviceProviders(mergeProviders(first.getServiceProviders(), second.getServiceProviders()))
                .build();
    }

    private List<BusinessLineProviderResponse> mergeProviders(
            List<BusinessLineProviderResponse> first,
            List<BusinessLineProviderResponse> second) {
        Map<String, BusinessLineProviderResponse> providersByCode = new LinkedHashMap<>();
        first.forEach(provider -> providersByCode.put(provider.getServiceProviderCode(), provider));
        second.forEach(provider -> providersByCode.merge(
                provider.getServiceProviderCode(), provider, this::mergeProviderResponses));
        return List.copyOf(providersByCode.values());
    }

    private BusinessLineProviderResponse mergeProviderResponses(
            BusinessLineProviderResponse first,
            BusinessLineProviderResponse second) {
        Map<String, BusinessLineServiceResponse> servicesByItem = new LinkedHashMap<>();
        first.getServices().forEach(service -> servicesByItem.put(service.getRmsItemCode(), service));
        second.getServices().forEach(service -> servicesByItem.putIfAbsent(service.getRmsItemCode(), service));
        return BusinessLineProviderResponse.builder()
                .serviceProviderCode(first.getServiceProviderCode())
                .rucProvider(first.getRucProvider())
                .providerName(first.getProviderName())
                .active(first.isActive() || second.isActive())
                .services(List.copyOf(servicesByItem.values()))
                .build();
    }

    private BusinessLineProviderResponse toProviderResponse(
            ServiceProvider provider,
            BusinessLinesRequest request) {
        Map<String, BusinessLineServiceResponse> servicesByItem = provider.getServices().stream()
                .filter(service -> isVisibleService(service, request))
                .map(service -> toServiceResponse(service, provider.getProviderName()))
                .collect(Collectors.toMap(
                        BusinessLineServiceResponse::getRmsItemCode,
                        service -> service,
                        (first, duplicate) -> first,
                        LinkedHashMap::new));
        return BusinessLineProviderResponse.builder()
                .serviceProviderCode(provider.getServiceProviderCode())
                .rucProvider(provider.getRucProvider())
                .providerName(provider.getProviderName())
                .active(provider.isActive())
                .services(List.copyOf(servicesByItem.values()))
                .build();
    }

    private boolean isVisibleService(ServiceDefinition service, BusinessLinesRequest request) {
        return request.getMovementTypeFilter() == null
                || service.getMovementType() == request.getMovementTypeFilter();
    }

    private BusinessLineServiceResponse toServiceResponse(ServiceDefinition service, String providerName) {
        return BusinessLineServiceResponse.builder()
                .rmsItemCode(service.getRmsItemCode())
                .description(service.getDescription())
                .active(service.isActive())
                .jdeCode(service.getJdeCode())
                .movementType(service.getMovementType().name())
                .mixedPayment(service.isMixedPayment())
                .flgItem(service.getFlgItem().name())
                .only(service.isOnly())
                .allowOtherBillableServices(service.isAllowOtherBillableServices())
                .allowSameService(service.isAllowSameService())
                .unique(service.isUnique())
                .serviceType(service.getServiceType())
                .recTelepeajeActive(service.isRecTelepeajeActive())
                .printConfirmationVoucher(service.isPrintConfirmationVoucher())
                .refund(service.isRefund())
                .minAmount(service.getMinAmount().toPlainString())
                .maxAmount(service.getMaxAmount().toPlainString())
                .timeoutWsMax(service.getTimeoutWsMax())
                .retriesWsMax(service.getRetriesWsMax())
                .numTickets(service.getNumTickets() == null ? DEFAULT_NUM_TICKETS : service.getNumTickets())
                .capabilities(service.getCapabilities().stream().map(Enum::name).collect(Collectors.toList()))
                .inputFields(service.getInputFields() == null
                        ? Collections.emptyList()
                        : service.getInputFields().stream().map(this::toInputFieldResponse).collect(Collectors.toList()))
                .paymentMethods(service.getPaymentMethods() == null
                        ? Collections.emptyList()
                        : service.getPaymentMethods().stream().map(this::toPaymentMethodResponse).collect(Collectors.toList()))
                .requiresConsent(service.isRequiresConsent())
                .consentText(formatConsentText(service, providerName))
                .build();
    }

    private String formatConsentText(ServiceDefinition service, String providerName) {
        if (!service.isRequiresConsent() || service.getConsentText() == null) {
            return service.getConsentText();
        }
        String resolvedProviderName = providerName == null ? "" : providerName;
        String consentText = service.getConsentText().replace(PROVIDER_NAME_PLACEHOLDER, resolvedProviderName);
        return wrapText(consentText, appProperties.getBusinessLines().getConsentTextMaxLineLength());
    }

    private String wrapText(String text, int maxLineLength) {
        if (text.isBlank() || maxLineLength < 1) {
            return text;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder formattedText = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxLineLength) {
                currentLine.append(' ').append(word);
            } else {
                appendLine(formattedText, currentLine);
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }

        appendLine(formattedText, currentLine);
        return formattedText.toString();
    }

    private void appendLine(StringBuilder formattedText, StringBuilder line) {
        if (line.isEmpty()) {
            return;
        }
        if (!formattedText.isEmpty()) {
            formattedText.append('\n');
        }
        formattedText.append(line);
    }

    private BusinessLineInputFieldResponse toInputFieldResponse(InputField inputField) {
        return BusinessLineInputFieldResponse.builder()
                .id(inputField.getId())
                .label(inputField.getLabel())
                .type(inputField.getType().name())
                .capability(inputField.getCapability())
                .required(inputField.isRequired())
                .group(inputField.getGroup())
                .conditional(inputField.getConditional())
                .length(inputField.getLength())
                .regex(inputField.getRegex())
                .groupLength(inputField.getGroupLength())
                .build();
    }

    private BusinessLinePaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        return BusinessLinePaymentMethodResponse.builder()
                .servicePaymentMethodId(paymentMethod.getServicePaymentMethodId())
                .paymentMethodCode(paymentMethod.getPaymentMethodCode().name().toUpperCase(Locale.ROOT).replace('_', ' '))
                .active(paymentMethod.isActive())
                .build();
    }

    private record SubcategoryResponseKey(String categoryCode, String subcategoryCode) {
    }
}

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
import java.util.List;
import java.util.Locale;
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

    private final BusinessLinesCatalogCacheService businessLinesCatalogCacheService;
    private final AppProperties appProperties;

    @Override
    public BusinessLinesResponse getBusinessLines(BusinessLinesRequest request) {
        List<BusinessLineCollectionSubcategoryResponse> collectionSubcategories = businessLinesCatalogCacheService.getCatalogSnapshot(request)
                .getCategories().stream()
                .flatMap(category -> category.getSubcategories().stream()
                        .map(subcategory -> toCollectionSubcategoryResponse(category.getCategoryCode(), category.getCategoryName(), subcategory, request)))
                .filter(subcategory -> !subcategory.getServiceProviders().isEmpty())
                .collect(Collectors.toList());

        return ResponseFactory.businessLines(request, collectionSubcategories);
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
                .serviceProviders(subcategory.getProviders().stream()
                        .map(provider -> toProviderResponse(provider, request))
                        .filter(provider -> !provider.getServices().isEmpty())
                        .collect(Collectors.toList()))
                .build();
    }

    private BusinessLineProviderResponse toProviderResponse(ServiceProvider provider, BusinessLinesRequest request) {
        return BusinessLineProviderResponse.builder()
                .serviceProviderCode(provider.getServiceProviderCode())
                .rucProvider(provider.getRucProvider())
                .providerName(provider.getProviderName())
                .active(provider.isActive())
                .services(provider.getServices().stream()
                        .filter(service -> request.getMovementTypeFilter() == null
                                || service.getMovementType() == request.getMovementTypeFilter())
                        .map(this::toServiceResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private BusinessLineServiceResponse toServiceResponse(ServiceDefinition service) {
        return BusinessLineServiceResponse.builder()
                .rmsItemCode(service.getRmsItemCode())
                .description(service.getDescription())
                .active(service.isActive())
                .jdeCode(service.getJdeCode())
                .movementType(service.getMovementType().name())
                .mixedPayment(service.isMixedPayment())
                .flgItem(service.getFlgItem().name())
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
                .consentText(formatConsentText(service))
                .build();
    }

    private String formatConsentText(ServiceDefinition service) {
        if (!service.isRequiresConsent() || service.getConsentText() == null) {
            return service.getConsentText();
        }
        return wrapText(service.getConsentText(), appProperties.getBusinessLines().getConsentTextMaxLineLength());
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
                .build();
    }

    private BusinessLinePaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        return BusinessLinePaymentMethodResponse.builder()
                .servicePaymentMethodId(paymentMethod.getServicePaymentMethodId())
                .paymentMethodCode(paymentMethod.getPaymentMethodCode().name().toUpperCase(Locale.ROOT).replace('_', ' '))
                .active(paymentMethod.isActive())
                .build();
    }
}

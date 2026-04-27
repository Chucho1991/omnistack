package com.omnistack.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BusinessLinesServiceTest {

    @Test
    void shouldReturnCatalogFromCache() {
        BusinessLinesCatalogCacheService cacheService = Mockito.mock(BusinessLinesCatalogCacheService.class);
        BusinessLinesService service = new BusinessLinesService(cacheService);
        BusinessLinesRequest request = BusinessLinesRequest.builder()
                .chain("001")
                .store("0001")
                .storeName("Tienda Centro")
                .pos("POS-01")
                .channelPos(ChannelPos.POS)
                .movementTypeFilter(MovementType.CASH_IN)
                .build();

        ServiceDefinition cashInService = ServiceDefinition.builder()
                .categoryCode("REC")
                .subcategoryCode("CEL")
                .serviceProviderCode("CLARO")
                .rmsItemCode("900001")
                .description("Recarga Claro")
                .active(true)
                .jdeCode("JDE-REC-001")
                .movementType(MovementType.CASH_IN)
                .mixedPayment(false)
                .flgItem(FlgItem.RECA)
                .refund(false)
                .minAmount(new BigDecimal("1.00"))
                .maxAmount(new BigDecimal("200.00"))
                .timeoutWsMax("10000")
                .retriesWsMax("3")
                .numTickets("3")
                .capabilities(List.of(Capability.PRECHECK, Capability.EXECUTE))
                .inputFields(List.of(InputField.builder()
                        .id("phone")
                        .label("Telefono")
                        .type(InputFieldType.STRING)
                        .capability(Capability.PRECHECK.name())
                        .required(true)
                        .group("PHONE")
                        .build()))
                .paymentMethods(List.of(PaymentMethod.builder()
                        .servicePaymentMethodId(1)
                        .paymentMethodCode(PaymentMethodCode.EFECTIVO)
                        .active(true)
                        .description("Pago en efectivo")
                        .build()))
                .requiresConsent(false)
                .build();

        ServiceDefinition cashOutService = ServiceDefinition.builder()
                .categoryCode("REC")
                .subcategoryCode("CEL")
                .serviceProviderCode("CLARO")
                .rmsItemCode("900002")
                .description("Retiro Claro")
                .active(true)
                .jdeCode("JDE-REC-002")
                .movementType(MovementType.CASH_OUT)
                .mixedPayment(false)
                .flgItem(FlgItem.RECA)
                .refund(true)
                .minAmount(new BigDecimal("1.00"))
                .maxAmount(new BigDecimal("200.00"))
                .timeoutWsMax("10000")
                .retriesWsMax("3")
                .capabilities(List.of(Capability.EXECUTE))
                .paymentMethods(List.of())
                .requiresConsent(false)
                .build();

        when(cacheService.getCatalogSnapshot(request)).thenReturn(CatalogSnapshot.builder()
                .categories(List.of(Category.builder()
                        .categoryCode("REC")
                        .categoryName("Recargas")
                        .subcategories(List.of(CollectionSubcategory.builder()
                                .subcategoryCode("CEL")
                                .subcategoryName("Recargas celulares")
                                .active(true)
                                .providers(List.of(ServiceProvider.builder()
                                        .serviceProviderCode("CLARO")
                                        .providerName("Claro")
                                        .active(true)
                                        .services(List.of(cashInService, cashOutService))
                                        .build()))
                                .build()))
                        .build()))
                .services(List.of(cashInService, cashOutService))
                .loadedAt(OffsetDateTime.now())
                .version("v1")
                .build());

        var response = service.getBusinessLines(request);

        assertEquals("001", response.getChain());
        assertEquals("0001", response.getStore());
        assertEquals("Tienda Centro", response.getStoreName());
        assertEquals("POS", response.getChannelPos());
        assertEquals(1, response.getCollectionSubcategory().size());
        assertEquals("REC", response.getCollectionSubcategory().get(0).getCategoryCode());
        assertTrue(response.getCollectionSubcategory().get(0).isActive());
        assertEquals(1, response.getCollectionSubcategory().get(0).getServiceProviders().size());
        assertEquals(1, response.getCollectionSubcategory().get(0).getServiceProviders().get(0).getServices().size());
        assertEquals("900001", response.getCollectionSubcategory().get(0).getServiceProviders().get(0).getServices().get(0).getRmsItemCode());
        assertEquals("10000", response.getCollectionSubcategory().get(0).getServiceProviders().get(0).getServices().get(0).getTimeoutWsMax());
        assertEquals("3", response.getCollectionSubcategory().get(0).getServiceProviders().get(0).getServices().get(0).getRetriesWsMax());
        assertEquals("3", response.getCollectionSubcategory().get(0).getServiceProviders().get(0).getServices().get(0).getNumTickets());
        assertFalse(response.getCollectionSubcategory().get(0).getServiceProviders().get(0).getServices().get(0).isRequiresConsent());
        assertEquals("phone", response.getCollectionSubcategory().get(0).getServiceProviders().get(0).getServices().get(0).getInputFields().get(0).getId());
        assertEquals("EFECTIVO", response.getCollectionSubcategory().get(0).getServiceProviders().get(0).getServices().get(0).getPaymentMethods().get(0).getPaymentMethodCode());
    }
}

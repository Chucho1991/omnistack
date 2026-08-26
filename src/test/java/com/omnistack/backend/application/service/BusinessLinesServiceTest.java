package com.omnistack.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.config.properties.AppProperties;
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
        BusinessLinesService service = new BusinessLinesService(cacheService, new AppProperties());
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
                .consentText("Texto sin formato requerido")
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
                                        .rucProvider("9999999999001")
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
        assertEquals(1, response.getCategories().size());
        assertEquals("REC", response.getCategories().get(0).getCategoryCode());
        assertEquals(1, response.getCategories().get(0).getSubcategories().size());
        var subcategory = response.getCategories().get(0).getSubcategories().get(0);
        assertTrue(subcategory.isActive());
        assertEquals(1, subcategory.getServiceProviders().size());
        assertEquals("9999999999001", subcategory.getServiceProviders().get(0).getRucProvider());
        assertEquals(1, subcategory.getServiceProviders().get(0).getServices().size());
        assertEquals("900001", subcategory.getServiceProviders().get(0).getServices().get(0).getRmsItemCode());
        assertEquals("10000", subcategory.getServiceProviders().get(0).getServices().get(0).getTimeoutWsMax());
        assertEquals("3", subcategory.getServiceProviders().get(0).getServices().get(0).getRetriesWsMax());
        assertEquals("3", subcategory.getServiceProviders().get(0).getServices().get(0).getNumTickets());
        assertEquals("RECA", subcategory.getServiceProviders().get(0).getServices().get(0).getFlagItem());
        assertFalse(subcategory.getServiceProviders().get(0).getServices().get(0).isOnly());
        assertTrue(subcategory.getServiceProviders().get(0).getServices().get(0).isAllowOtherBillableServices());
        assertTrue(subcategory.getServiceProviders().get(0).getServices().get(0).isAllowSameService());
        assertEquals("R", subcategory.getServiceProviders().get(0).getServices().get(0).getServiceType());
        assertTrue(subcategory.getServiceProviders().get(0).getServices().get(0).isRecTelepeajeActive());
        assertTrue(subcategory.getServiceProviders().get(0).getServices().get(0).isPrintConfirmationVoucher());
        assertFalse(subcategory.getServiceProviders().get(0).getServices().get(0).isRequiresConsent());
        assertEquals("Texto sin formato requerido", subcategory.getServiceProviders().get(0).getServices().get(0).getConsentText());
        assertEquals("phone", subcategory.getServiceProviders().get(0).getServices().get(0).getInputFields().get(0).getId());
        assertEquals("EFECTIVO", subcategory.getServiceProviders().get(0).getServices().get(0).getPaymentMethods().get(0).getPaymentMethodCode());
    }

    @Test
    void shouldKeepSubcategoriesGroupedUnderTheirCategory() {
        BusinessLinesCatalogCacheService cacheService = Mockito.mock(BusinessLinesCatalogCacheService.class);
        BusinessLinesService service = new BusinessLinesService(cacheService, new AppProperties());
        BusinessLinesRequest request = BusinessLinesRequest.builder()
                .chain("001")
                .store("0001")
                .storeName("Tienda Centro")
                .pos("POS-01")
                .channelPos(ChannelPos.POS)
                .build();
        ServiceDefinition firstService = serviceDefinition("900001", MovementType.CASH_IN);
        ServiceDefinition secondService = serviceDefinition("900002", MovementType.CASH_OUT);

        when(cacheService.getCatalogSnapshot(request)).thenReturn(CatalogSnapshot.builder()
                .categories(List.of(Category.builder()
                        .categoryCode("ENT")
                        .categoryName("Entretenimiento")
                        .subcategories(List.of(
                                subcategory("BET", "Apuestas", firstService),
                                subcategory("LOT", "Loterias", secondService)))
                        .build()))
                .services(List.of(firstService, secondService))
                .loadedAt(OffsetDateTime.now())
                .version("v1")
                .build());

        var response = service.getBusinessLines(request);

        assertEquals(1, response.getCategories().size());
        assertEquals("ENT", response.getCategories().get(0).getCategoryCode());
        assertEquals(List.of("BET", "LOT"), response.getCategories().get(0).getSubcategories().stream()
                .map(subcategoryResponse -> subcategoryResponse.getSubcategoryCode())
                .toList());
    }

    @Test
    void shouldFormatConsentTextWithoutCuttingWordsWhenConsentIsRequired() {
        BusinessLinesCatalogCacheService cacheService = Mockito.mock(BusinessLinesCatalogCacheService.class);
        AppProperties appProperties = new AppProperties();
        appProperties.getBusinessLines().setConsentTextMaxLineLength(20);
        BusinessLinesService service = new BusinessLinesService(cacheService, appProperties);
        BusinessLinesRequest request = BusinessLinesRequest.builder()
                .chain("001")
                .store("0001")
                .storeName("Tienda Centro")
                .pos("POS-01")
                .channelPos(ChannelPos.POS)
                .build();
        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
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
                .capabilities(List.of(Capability.EXECUTE))
                .inputFields(List.of())
                .paymentMethods(List.of())
                .requiresConsent(true)
                .consentText("Autorizo de forma expresa la creacion de mi registro")
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
                                        .rucProvider("9999999999001")
                                        .providerName("Claro")
                                        .active(true)
                                        .services(List.of(serviceDefinition))
                                        .build()))
                                .build()))
                        .build()))
                .services(List.of(serviceDefinition))
                .loadedAt(OffsetDateTime.now())
                .version("v1")
                .build());

        var response = service.getBusinessLines(request);

        String consentText = response.getCategories().get(0).getSubcategories().get(0)
                .getServiceProviders().get(0).getServices().get(0).getConsentText();
        assertEquals("Autorizo de forma\nexpresa la creacion\nde mi registro", consentText);
        assertTrue(consentText.lines().allMatch(line -> line.length() <= 20));
    }

    @Test
    void shouldResolveProviderNamePlaceholderInConsentText() {
        BusinessLinesCatalogCacheService cacheService = Mockito.mock(BusinessLinesCatalogCacheService.class);
        AppProperties appProperties = new AppProperties();
        appProperties.getBusinessLines().setConsentTextMaxLineLength(200);
        BusinessLinesService service = new BusinessLinesService(cacheService, appProperties);
        BusinessLinesRequest request = BusinessLinesRequest.builder()
                .chain("001")
                .store("0001")
                .storeName("Tienda Centro")
                .pos("POS-01")
                .channelPos(ChannelPos.POS)
                .build();
        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .categoryCode("REC")
                .subcategoryCode("BET")
                .serviceProviderCode("ECUABET")
                .rmsItemCode("900001")
                .description("Recarga Ecuabet")
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
                .capabilities(List.of(Capability.EXECUTE))
                .inputFields(List.of())
                .paymentMethods(List.of())
                .requiresConsent(true)
                .consentText("Autorizo servicios digitales de {{provider_name}}")
                .build();

        when(cacheService.getCatalogSnapshot(request)).thenReturn(CatalogSnapshot.builder()
                .categories(List.of(Category.builder()
                        .categoryCode("REC")
                        .categoryName("Recargas")
                        .subcategories(List.of(CollectionSubcategory.builder()
                                .subcategoryCode("BET")
                                .subcategoryName("PRONOSTICOS DEPORTIVOSCOS DEPORTIVOS")
                                .active(true)
                                .providers(List.of(ServiceProvider.builder()
                                        .serviceProviderCode("ECUABET")
                                        .rucProvider("9999999999001")
                                        .providerName("ECUABET")
                                        .active(true)
                                        .services(List.of(serviceDefinition))
                                        .build()))
                                .build()))
                        .build()))
                .services(List.of(serviceDefinition))
                .loadedAt(OffsetDateTime.now())
                .version("v1")
                .build());

        var response = service.getBusinessLines(request);

        String consentText = response.getCategories().get(0).getSubcategories().get(0)
                .getServiceProviders().get(0).getServices().get(0).getConsentText();
        assertEquals("Autorizo servicios digitales de ECUABET", consentText);
    }

    @Test
    void shouldReturnAllCatalogServicesWithoutPropertyFilter() {
        BusinessLinesCatalogCacheService cacheService = Mockito.mock(BusinessLinesCatalogCacheService.class);
        AppProperties appProperties = new AppProperties();
        BusinessLinesService service = new BusinessLinesService(cacheService, appProperties);
        BusinessLinesRequest request = BusinessLinesRequest.builder()
                .chain("001")
                .store("0001")
                .storeName("Tienda Centro")
                .pos("POS-01")
                .channelPos(ChannelPos.POS)
                .build();

        List<ServiceDefinition> services = List.of(
                serviceDefinition("100713841", MovementType.CASH_IN),
                serviceDefinition("100708846", MovementType.CASH_OUT),
                serviceDefinition("100708850", MovementType.CASH_IN),
                serviceDefinition("100708848", MovementType.CASH_OUT),
                serviceDefinition("999999999", MovementType.CASH_IN));

        when(cacheService.getCatalogSnapshot(request)).thenReturn(CatalogSnapshot.builder()
                .categories(List.of(Category.builder()
                        .categoryCode("REC")
                        .categoryName("Recargas")
                        .subcategories(List.of(CollectionSubcategory.builder()
                                .subcategoryCode("BET")
                                .subcategoryName("PRONOSTICOS DEPORTIVOS")
                                .active(true)
                                .providers(List.of(ServiceProvider.builder()
                                        .serviceProviderCode("ECUABET")
                                        .rucProvider("9999999999001")
                                        .providerName("ECUABET")
                                        .active(true)
                                        .services(services)
                                        .build()))
                                .build()))
                        .build()))
                .services(services)
                .loadedAt(OffsetDateTime.now())
                .version("v1")
                .build());

        var response = service.getBusinessLines(request);

        List<String> returnedItems = response.getCategories().get(0).getSubcategories().get(0)
                .getServiceProviders().get(0)
                .getServices().stream()
                .map(serviceResponse -> serviceResponse.getRmsItemCode())
                .toList();
        assertEquals(List.of("100713841", "100708846", "100708850", "100708848", "999999999"), returnedItems);
    }

    private static CollectionSubcategory subcategory(
            String code,
            String name,
            ServiceDefinition service) {
        return CollectionSubcategory.builder()
                .subcategoryCode(code)
                .subcategoryName(name)
                .active(true)
                .providers(List.of(ServiceProvider.builder()
                        .serviceProviderCode("PROVIDER-" + code)
                        .providerName("Proveedor " + code)
                        .active(true)
                        .services(List.of(service))
                        .build()))
                .build();
    }

    private static ServiceDefinition serviceDefinition(String rmsItemCode, MovementType movementType) {
        return ServiceDefinition.builder()
                .categoryCode("REC")
                .subcategoryCode("BET")
                .serviceProviderCode("ECUABET")
                .rmsItemCode(rmsItemCode)
                .description("Servicio " + rmsItemCode)
                .active(true)
                .jdeCode("JDE-" + rmsItemCode)
                .movementType(movementType)
                .mixedPayment(false)
                .flgItem(FlgItem.RECA)
                .refund(false)
                .minAmount(new BigDecimal("1.00"))
                .maxAmount(new BigDecimal("200.00"))
                .timeoutWsMax("10000")
                .retriesWsMax("3")
                .numTickets("3")
                .capabilities(List.of(Capability.EXECUTE))
                .inputFields(List.of())
                .paymentMethods(List.of())
                .requiresConsent(false)
                .build();
    }

}

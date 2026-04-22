package com.omnistack.backend.infrastructure.adapter.catalog;

import com.omnistack.backend.application.port.out.CatalogSourcePort;
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
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Fuente en memoria para catálogos en esta primera etapa.
 */
@Component
public class InMemoryCatalogSourceAdapter implements CatalogSourcePort {

    @Override
    public CatalogSnapshot loadCatalogSnapshot() {
        ServiceDefinition claroRecarga = ServiceDefinition.builder()
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
                .capabilities(List.of(Capability.PRECHECK, Capability.EXECUTE, Capability.VERIFY, Capability.REVERSE))
                .inputFields(List.of(
                        InputField.builder().id("phone").label("Telefono").type(InputFieldType.STRING).capability(Capability.PRECHECK.name()).required(true).group("PHONE").build(),
                        InputField.builder().id("amount").label("Monto").type(InputFieldType.DOUBLE).capability(Capability.PRECHECK.name()).required(true).group("AMOUNT").build()))
                .paymentMethods(List.of(
                        PaymentMethod.builder().servicePaymentMethodId(1).paymentMethodCode(PaymentMethodCode.EFECTIVO).active(true).description("Pago en efectivo").build()))
                .requiresConsent(false)
                .consentText(null)
                .build();

        ServiceProvider provider = ServiceProvider.builder()
                .serviceProviderCode("CLARO")
                .providerName("Claro")
                .active(true)
                .services(List.of(claroRecarga))
                .build();

        CollectionSubcategory subcategory = CollectionSubcategory.builder()
                .subcategoryCode("CEL")
                .subcategoryName("Recargas celulares")
                .active(true)
                .providers(List.of(provider))
                .build();

        Category category = Category.builder()
                .categoryCode("REC")
                .categoryName("Recargas")
                .subcategories(List.of(subcategory))
                .build();

        return CatalogSnapshot.builder()
                .categories(List.of(category))
                .services(List.of(claroRecarga))
                .loadedAt(OffsetDateTime.now())
                .version("memory-v1")
                .build();
    }
}

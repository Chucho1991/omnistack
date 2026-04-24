package com.omnistack.backend.domain.model;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * Estado vigente del token asociado a un proveedor.
 */
@Value
@Builder
public class ProviderToken {
    String categoryCode;
    String subcategoryCode;
    String serviceProviderCode;
    String providerName;
    String token;
    OffsetDateTime refreshedAt;
    OffsetDateTime expiresAt;
}

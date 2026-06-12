package com.omnistack.backend.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegistroTrx {
    String uuid;
    String chain;
    String store;
    String storeName;
    String pos;
    String canal;
    String proveedor;
    String categoryCode;
    String subcategoryCode;
    String serviceProviderCode;
    String rmsItemCode;
    String capability;
    String authorization;
    BigDecimal monto;
    String moneda;
    String codEstado;
}

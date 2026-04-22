package com.omnistack.backend.infrastructure.adapter.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Proveedor de sentencias SQL para el endpoint business-lines.
 */
@Component
public class OracleBusinessLinesSqlProvider {

    private final String categorySubcategorySql = readSql("sql/business-lines/oracle/category-subcategory.sql");
    private final String serviceProvidersSql = readSql("sql/business-lines/oracle/service-providers.sql");
    private final String servicesSql = readSql("sql/business-lines/oracle/services.sql");
    private final String capabilitiesSql = readSql("sql/business-lines/oracle/capabilities.sql");
    private final String inputFieldsSql = readSql("sql/business-lines/oracle/input-fields.sql");
    private final String paymentMethodsSql = readSql("sql/business-lines/oracle/payment-methods.sql");

    public String getCategorySubcategorySql() {
        return categorySubcategorySql;
    }

    public String getServiceProvidersSql() {
        return serviceProvidersSql;
    }

    public String getServicesSql() {
        return servicesSql;
    }

    public String getCapabilitiesSql() {
        return capabilitiesSql;
    }

    public String getInputFieldsSql() {
        return inputFieldsSql;
    }

    public String getPaymentMethodsSql() {
        return paymentMethodsSql;
    }

    private String readSql(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try {
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return stripUtf8Bom(sql);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo cargar el recurso SQL: " + path, exception);
        }
    }

    private String stripUtf8Bom(String sql) {
        if (sql != null && !sql.isEmpty() && sql.charAt(0) == '\uFEFF') {
            return sql.substring(1);
        }
        return sql;
    }
}

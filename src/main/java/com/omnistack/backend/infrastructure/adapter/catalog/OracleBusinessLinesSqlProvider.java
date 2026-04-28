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

    /**
     * Retorna el SQL de categorias y subcategorias.
     *
     * @return sentencia SQL de categorias y subcategorias
     */
    public String getCategorySubcategorySql() {
        return categorySubcategorySql;
    }

    /**
     * Retorna el SQL de proveedores de servicio.
     *
     * @return sentencia SQL de proveedores
     */
    public String getServiceProvidersSql() {
        return serviceProvidersSql;
    }

    /**
     * Retorna el SQL de servicios.
     *
     * @return sentencia SQL de servicios
     */
    public String getServicesSql() {
        return servicesSql;
    }

    /**
     * Retorna el SQL de capacidades.
     *
     * @return sentencia SQL de capacidades
     */
    public String getCapabilitiesSql() {
        return capabilitiesSql;
    }

    /**
     * Retorna el SQL de campos de entrada.
     *
     * @return sentencia SQL de campos de entrada
     */
    public String getInputFieldsSql() {
        return inputFieldsSql;
    }

    /**
     * Retorna el SQL de metodos de pago.
     *
     * @return sentencia SQL de metodos de pago
     */
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

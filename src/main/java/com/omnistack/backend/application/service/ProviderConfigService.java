package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.ProviderConfigPort;
import com.omnistack.backend.config.properties.AppProperties;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicacion para resolucion de configuracion funcional de proveedores.
 * Carga el mapa completo de IN_OMNI_PROVEEDOR_CONFIG al arrancar y sirve de cache en memoria.
 * Clave de cache: "providerkey|config_key"
 *
 * Reemplaza la lectura de AppProperties.Integration.providers en strategies y adapters.
 */
@Slf4j
@Service
public class ProviderConfigService {

    private final ProviderConfigPort configPort;
    private Map<String, String> cache = Collections.emptyMap();

    public ProviderConfigService(@Autowired(required = false) ProviderConfigPort configPort) {
        this.configPort = configPort;
    }

    @PostConstruct
    public void init() {
        if (configPort != null) {
            cache = Collections.unmodifiableMap(configPort.loadAll());
        } else {
            log.warn("ProviderConfigPort no disponible — config de proveedores no se cargara desde DB");
        }
    }

    /**
     * Construye un AppProperties.ProviderProperties poblado con los valores de DB.
     * Retorna null si el proveedor no tiene ninguna config registrada.
     */
    public AppProperties.ProviderProperties getProviderProperties(String providerKey) {
        if (!hasProvider(providerKey)) return null;

        AppProperties.ProviderProperties p = new AppProperties.ProviderProperties();
        p.setTechnicalUser(getString(providerKey, "technical_user"));
        p.setProviderName(getString(providerKey, "provider_name"));
        p.setCategoryCode(getString(providerKey, "category_code"));
        p.setSubcategoryCode(getString(providerKey, "subcategory_code"));
        p.setServiceProviderCode(getString(providerKey, "service_provider_code"));
        p.setToken(getString(providerKey, "token"));
        p.setShopId(getString(providerKey, "shop_id"));
        p.setShopIp(getString(providerKey, "shop_ip"));
        Integer country = getInteger(providerKey, "country");
        if (country != null) p.setCountry(country);
        p.setCanal(getString(providerKey, "canal"));
        p.setLatitude(getString(providerKey, "latitude"));
        p.setLongitude(getString(providerKey, "longitude"));
        p.setCanton(getString(providerKey, "canton"));
        p.setProvince(getString(providerKey, "province"));
        p.setParish(getString(providerKey, "parish"));

        Integer puntoOp = getInteger(providerKey, "punto_operacion_id");
        if (puntoOp != null) p.setPuntoOperacionId(puntoOp);

        Integer medioId = getInteger(providerKey, "medio_id");
        if (medioId != null) p.setMedioId(medioId);

        Integer clienteId = getInteger(providerKey, "cliente_id");
        if (clienteId != null) p.setClienteId(clienteId);

        Long ttlHours = getLong(providerKey, "auth_ttl_hours");
        AppProperties.ProviderTokenProperties auth = p.getAuth();
        auth.setMode(getStringOrDefault(providerKey, "auth_mode", "STATIC"));
        if (ttlHours != null) auth.setTtlHours(ttlHours);
        auth.setRefreshOnStartup(
                Boolean.parseBoolean(getStringOrDefault(providerKey, "auth_refresh_on_startup", "true")));

        AppProperties.ProviderLoginProperties login = auth.getLogin();
        login.setUsername(getString(providerKey, "auth_username"));
        login.setPassword(getString(providerKey, "auth_password"));
        login.setProductToSell(getString(providerKey, "auth_product_to_sell"));

        return p;
    }

    /**
     * Retorna el valor de texto para el proveedor y clave dados.
     */
    public Optional<String> findString(String providerKey, String configKey) {
        return Optional.ofNullable(cache.get(cacheKey(providerKey, configKey)));
    }

    public String getString(String providerKey, String configKey) {
        return cache.get(cacheKey(providerKey, configKey));
    }

    public String getStringOrDefault(String providerKey, String configKey, String defaultValue) {
        String val = getString(providerKey, configKey);
        return val != null ? val : defaultValue;
    }

    public Integer getInteger(String providerKey, String configKey) {
        String val = getString(providerKey, configKey);
        if (val == null || val.isBlank()) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("ProviderConfig: valor no numerico para {}: {}", cacheKey(providerKey, configKey), val);
            return null;
        }
    }

    public Long getLong(String providerKey, String configKey) {
        String val = getString(providerKey, configKey);
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            log.warn("ProviderConfig: valor no long para {}: {}", cacheKey(providerKey, configKey), val);
            return null;
        }
    }

    /**
     * Retorna todas las claves de proveedor presentes en el cache.
     */
    public Set<String> allProviderKeys() {
        return cache.keySet().stream()
                .map(k -> k.split("\\|")[0])
                .collect(Collectors.toSet());
    }

    private boolean hasProvider(String providerKey) {
        String prefix = providerKey.toLowerCase() + "|";
        return cache.keySet().stream().anyMatch(k -> k.startsWith(prefix));
    }

    /**
     * Traduce un valor interno (origen del POS) al valor externo que espera el proveedor.
     * Busca la fila donde VALOR_ORIGEN = valorOrigen para el proveedor y campo dados.
     * Retorna null si no existe mapeo para ese valor de origen.
     *
     * Ejemplo: mapValue("claro", "company_id", "1") → "2"  (Fybeca→CLARO)
     */
    public String mapValue(String providerKey, String configKey, String valorOrigen) {
        if (valorOrigen == null) return null;
        return cache.get(cacheKey(providerKey, configKey) + "|" + valorOrigen.toLowerCase());
    }

    private static String cacheKey(String providerKey, String configKey) {
        return providerKey.toLowerCase() + "|" + configKey.toLowerCase();
    }
}

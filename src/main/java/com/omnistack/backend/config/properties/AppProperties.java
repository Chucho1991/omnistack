package com.omnistack.backend.config.properties;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades funcionales de la aplicacion.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Swagger swagger = new Swagger();
    private Catalog catalog = new Catalog();
    private BusinessLines businessLines = new BusinessLines();
    private Integrations integrations = new Integrations();
    private Datasource datasource = new Datasource();

    /**
     * Propiedades de metadata Swagger.
     */
    @Data
    public static class Swagger {
        private String title;
        private String description;
        private String version;
    }

    /**
     * Propiedades de catalogo transaccional.
     */
    @Data
    public static class Catalog {
        private Refresh refresh = new Refresh();

        /**
         * Propiedades de recarga periodica del catalogo.
         */
        @Data
        public static class Refresh {
            private long fixedDelayMs;
            private long initialDelayMs;
        }
    }

    /**
     * Propiedades de consulta de lineas de negocio.
     */
    @Data
    public static class BusinessLines {
        private String source = "oracle";
        private int consentTextMaxLineLength = 56;
        private Map<String, Integer> canalCodigos = new HashMap<>(Map.of("POS", 1));
        private Cache cache = new Cache();
        private DefaultRequest defaultRequest = new DefaultRequest();

        /**
         * Propiedades de cache de lineas de negocio.
         */
        @Data
        public static class Cache {
            private long ttlHours = 6;
        }

        /**
         * Valores por defecto para solicitudes de lineas de negocio.
         */
        @Data
        public static class DefaultRequest {
            private String chain;
            private String store;
            private String storeName;
            private String pos;
            private String channelPos;
        }
    }

    /**
     * Propiedades generales de integraciones externas.
     */
    @Data
    public static class Integrations {
        private int defaultConnectTimeoutMs = 60000;
        private int defaultReadTimeoutMs = 60000;
        private boolean mockEnabled;
        private boolean sslVerificationDisabled = false;
    }

    /**
     * Datasources Oracle de la aplicacion.
     */
    @Data
    public static class Datasource {
        private OracleDatasource prod = new OracleDatasource();
        private OracleDatasource rms = new OracleDatasource();

        @Data
        public static class OracleDatasource {
            private String url;
            private String username;
            private String password;
            private String driverClassName;
            private String schema;
        }
    }

    /**
     * Configuracion funcional de un proveedor externo.
     */
    @Data
    public static class ProviderProperties {
        private String technicalUser;
        private String providerName;
        private String categoryCode;
        private String subcategoryCode;
        private String serviceProviderCode;
        private String shopId;
        private String shopIp;
        private Integer country;
        private String token;
        private String canal;
        private Integer medioId;
        private Integer puntoOperacionId;
        private Integer clienteId;
        // CLARO-specific fields
        private String companyId;
        private String consumerId;
        private String channelId;
        private String mediaId;
        private String mediaDetailId;
        private String externalOperation;
        private String subscriberType;
        private String subscriptionType;
        private String codCaja;
        private String codSite;
        private String latitude;
        private String longitude;
        private String canton;
        private String province;
        private String parish;
        private Map<String, String> offerIds = new HashMap<>();
        private ProviderTokenProperties auth = new ProviderTokenProperties();
    }

    /**
     * Configuracion de token dinamico de proveedor.
     */
    @Data
    public static class ProviderTokenProperties {
        private String mode = "STATIC";
        private long ttlHours = 24;
        private boolean refreshOnStartup = true;
        private ProviderLoginProperties login = new ProviderLoginProperties();
    }

    /**
     * Configuracion de login para obtener token de proveedor.
     */
    @Data
    public static class ProviderLoginProperties {
        private String username;
        private String password;
        private String productToSell;
    }

}

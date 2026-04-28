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
    private Integration integration = new Integration();

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
        private Cache cache = new Cache();
        private DefaultRequest defaultRequest = new DefaultRequest();
        private Oracle oracle = new Oracle();

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

        /**
         * Propiedades Oracle para catalogo comercial.
         */
        @Data
        public static class Oracle {
            private Datasource datasource1 = new Datasource();

            /**
             * Propiedades de datasource Oracle.
             */
            @Data
            public static class Datasource {
                private String url;
                private String username;
                private String password;
                private String driverClassName;
            }
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
    }

    /**
     * Propiedades por proveedor externo.
     */
    @Data
    public static class Integration {
        private Map<String, ProviderProperties> providers = new HashMap<>();
    }

    /**
     * Configuracion funcional de un proveedor externo.
     */
    @Data
    public static class ProviderProperties {
        private String baseUrl;
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
        private ProviderTokenProperties auth = new ProviderTokenProperties();
        private Map<String, ProviderCapabilityProperties> services = new HashMap<>();
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
        private String path;
        private String username;
        private String password;
        private String productToSell;
    }

    /**
     * Configuracion de operaciones por capacidad y movimiento.
     */
    @Data
    public static class ProviderCapabilityProperties {
        private ProviderOperationProperties cashin = new ProviderOperationProperties();
        private ProviderOperationProperties cashout = new ProviderOperationProperties();
    }

    /**
     * Configuracion de una operacion externa concreta.
     */
    @Data
    public static class ProviderOperationProperties {
        private String item;
        private String path;
        private String capabilities;
        private String name;
    }
}

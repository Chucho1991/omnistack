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
    private Integrations integrations = new Integrations();
    private Integration integration = new Integration();

    @Data
    public static class Swagger {
        private String title;
        private String description;
        private String version;
    }

    @Data
    public static class Catalog {
        private Refresh refresh = new Refresh();

        @Data
        public static class Refresh {
            private long fixedDelayMs;
            private long initialDelayMs;
        }
    }

    @Data
    public static class Integrations {
        private int defaultConnectTimeoutMs;
        private int defaultReadTimeoutMs;
        private boolean mockEnabled;
    }

    @Data
    public static class Integration {
        private Map<String, ProviderProperties> providers = new HashMap<>();
    }

    @Data
    public static class ProviderProperties {
        private String baseUrl;
        private String technicalUser;
    }
}

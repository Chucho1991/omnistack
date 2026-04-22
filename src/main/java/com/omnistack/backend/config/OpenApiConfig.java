package com.omnistack.backend.config;

import com.omnistack.backend.config.properties.AppProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion principal de OpenAPI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI omnistackOpenApi(AppProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getSwagger().getTitle())
                        .description(properties.getSwagger().getDescription())
                        .version(properties.getSwagger().getVersion())
                        .contact(new Contact().name("OMNISTACK Backend Team")));
    }
}

package com.omnistack.backend.config;

import com.omnistack.backend.config.properties.AppProperties;
import java.time.Duration;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Configuracion base para clientes REST externos.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient omnistackWebClient(AppProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getIntegrations().getDefaultConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(properties.getIntegrations().getDefaultReadTimeoutMs()));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

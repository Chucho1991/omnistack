package com.omnistack.backend.config;

import com.omnistack.backend.config.properties.AppProperties;
import java.time.Duration;
import java.util.List;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;

/**
 * Configuracion base para clientes REST externos.
 */
@Configuration
public class WebClientConfig {

    private static final String DEFAULT_TLS_PROTOCOL = "TLSv1.2";

    /** Comprobantes (PDF en base64, ej. GenerarComprobanteVenta de Tradicionales) pueden superar
     * el default de Spring (256KB) para buffers en memoria del WebClient. */
    private static final int MAX_IN_MEMORY_SIZE_BYTES = 16 * 1024 * 1024;

    /**
     * Construye el cliente HTTP reactivo usado por adapters de integracion.
     *
     * @param properties propiedades de timeout de integraciones
     * @return cliente WebClient configurado
     */
    @Bean
    public WebClient omnistackWebClient(AppProperties properties) {
        AppProperties.Integrations integrations = properties.getIntegrations();
        Http11SslContextSpec sslContextSpec = Http11SslContextSpec.forClient()
                .configure(builder -> builder.protocols(resolveTlsProtocols(integrations)));
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, integrations.getDefaultConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(integrations.getDefaultReadTimeoutMs()))
                .secure(ssl -> ssl.sslContext(sslContextSpec));

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .build();
    }

    private String[] resolveTlsProtocols(AppProperties.Integrations integrations) {
        List<String> tlsProtocols = integrations.getTlsProtocols();
        if (tlsProtocols == null || tlsProtocols.isEmpty()) {
            return new String[] {DEFAULT_TLS_PROTOCOL};
        }

        String[] configuredProtocols = tlsProtocols.stream()
                .filter(protocol -> protocol != null && !protocol.isBlank())
                .map(String::trim)
                .toArray(String[]::new);

        if (configuredProtocols.length == 0) {
            return new String[] {DEFAULT_TLS_PROTOCOL};
        }

        return configuredProtocols;
    }
}

package com.omnistack.backend.config;

import com.omnistack.backend.config.properties.AppProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.time.Duration;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Configuracion base para clientes REST externos.
 */
@Slf4j
@Configuration
public class WebClientConfig {

    /**
     * Construye el cliente HTTP reactivo usado por adapters de integracion.
     * Cuando app.integrations.ssl-verification-disabled=true se deshabilita la
     * verificacion de certificados SSL — necesario para proveedores con TLS legacy
     * (ej: www8.loteria.com.ec usa cipher suites deshabilitados por Java 17).
     *
     * @param properties propiedades de timeout e integraciones
     * @return cliente WebClient configurado
     */
    @Bean
    public WebClient omnistackWebClient(AppProperties properties) throws SSLException {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getIntegrations().getDefaultConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(properties.getIntegrations().getDefaultReadTimeoutMs()));

        if (properties.getIntegrations().isSslVerificationDisabled()) {
            log.warn("SSL verification DISABLED — solo usar en entornos no productivos");
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            // www8.loteria.com.ec rechaza TLS 1.3 ClientHellos con handshake_failure;
            // forzar TLS 1.2 directamente en el SSLEngine resuelve la incompatibilidad.
            httpClient = httpClient.secure(spec -> spec
                    .sslContext(sslContext)
                    .handlerConfigurator((SslHandler h) ->
                            h.engine().setEnabledProtocols(new String[]{"TLSv1.2"})));
        }

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

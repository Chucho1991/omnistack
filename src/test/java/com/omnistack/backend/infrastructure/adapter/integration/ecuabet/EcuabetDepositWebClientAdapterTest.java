package com.omnistack.backend.infrastructure.adapter.integration.ecuabet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.model.EcuabetDepositCommand;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class EcuabetDepositWebClientAdapterTest {

    private final AtomicReference<String> capturedPath = new AtomicReference<>();
    private final AtomicReference<String> capturedBody = new AtomicReference<>();
    private final AtomicReference<String> capturedChain = new AtomicReference<>();
    private final AtomicReference<String> capturedStore = new AtomicReference<>();
    private final AtomicReference<String> capturedStoreName = new AtomicReference<>();
    private final AtomicReference<String> capturedPos = new AtomicReference<>();
    private final AtomicReference<String> capturedChannelPos = new AtomicReference<>();

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSendDepositPayloadToConfiguredEndpoint() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/user/deposit", exchange -> respondJson(exchange,
                """
                {
                  "error": 0,
                  "code": 0,
                  "transactionId": "41468",
                  "depositId": "563455",
                  "nombre": "Carlos",
                  "apellido": "Perez",
                  "currency": "USD"
                }
                """));
        server.start();

        EcuabetDepositWebClientAdapter adapter = new EcuabetDepositWebClientAdapter(
                WebClient.builder().build(),
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-test");

        var response = adapter.deposit(EcuabetDepositCommand.builder()
                .uuid("uuid-1")
                .chain("1")
                .store("148")
                .storeName("FYBECA EL BATAN")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("10001565826")
                .userid("997561")
                .document("0912345678")
                .amount(new BigDecimal("100000.00"))
                .transactionId(91081)
                .build(), "/user/deposit");

        assertEquals("/user/deposit", capturedPath.get());
        assertEquals("1", capturedChain.get());
        assertEquals("148", capturedStore.get());
        assertEquals("FYBECA EL BATAN", capturedStoreName.get());
        assertEquals("1", capturedPos.get());
        assertEquals("POS", capturedChannelPos.get());
        assertTrue(capturedBody.get().contains("\"shop\":\"998739\""));
        assertTrue(capturedBody.get().contains("\"token\":\"token-test\""));
        assertTrue(capturedBody.get().contains("\"userid\":997561"));
        assertTrue(capturedBody.get().contains("\"country\":66"));
        assertTrue(capturedBody.get().contains("\"amount\":100000.00"));
        assertTrue(capturedBody.get().contains("\"transactionId\":91081"));
        assertTrue(capturedBody.get().contains("\"shop_info\":\"FYBECA EL BATAN\""));
        assertTrue(capturedBody.get().contains("\"shop_ip\":\"10.0.0.10\""));
        assertEquals("91081", String.valueOf(response.getPayload().get("authorization")));
        assertEquals("563455", String.valueOf(response.getPayload().get("depositId")));
        assertEquals("41468", String.valueOf(response.getPayload().get("providerTransactionId")));
        assertEquals("Carlos", String.valueOf(response.getPayload().get("name")));
    }

    private void respondJson(HttpExchange exchange, String body) throws IOException {
        capturedPath.set(exchange.getRequestURI().getPath());
        capturedChain.set(exchange.getRequestHeaders().getFirst("chain"));
        capturedStore.set(exchange.getRequestHeaders().getFirst("store"));
        capturedStoreName.set(exchange.getRequestHeaders().getFirst("store_name"));
        capturedPos.set(exchange.getRequestHeaders().getFirst("pos"));
        capturedChannelPos.set(exchange.getRequestHeaders().getFirst("channel_POS"));
        capturedBody.set(readBody(exchange.getRequestBody()));

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String readBody(InputStream bodyStream) throws IOException {
        return new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private AppProperties appProperties(String baseUrl) {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setBaseUrl(baseUrl);
        provider.setShopId("998739");
        provider.setShopIp("10.0.0.10");
        provider.setCountry(66);
        provider.setServiceProviderCode("1");

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().getProviders().put("ecuabet", provider);
        return appProperties;
    }
}

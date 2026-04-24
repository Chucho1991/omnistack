package com.omnistack.backend.infrastructure.adapter.integration.ecuabet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetUserSearchCommand;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class EcuabetUserSearchWebClientAdapterTest {

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
    void shouldSendCashoutPayloadToConfiguredWithdrawalEndpoint() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/user/searchwithdraw", exchange -> respondJson(exchange,
                """
                {
                  "error": 0,
                  "code": "0",
                  "name": "USU FEMSA",
                  "currency": "USD",
                  "amount": "10",
                  "userId": "998765"
                }
                """));
        server.start();

        EcuabetUserSearchWebClientAdapter adapter = new EcuabetUserSearchWebClientAdapter(
                WebClient.builder().build(),
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-test");

        var response = adapter.searchUser(EcuabetUserSearchCommand.builder()
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .withdrawId("7667")
                .password("88422")
                .build(), "/user/searchwithdraw");

        assertEquals("/user/searchwithdraw", capturedPath.get());
        assertEquals("1", capturedChain.get());
        assertEquals("148", capturedStore.get());
        assertEquals("FYBECA AMAZONAS", capturedStoreName.get());
        assertEquals("1", capturedPos.get());
        assertEquals("POS", capturedChannelPos.get());
        assertTrue(capturedBody.get().contains("\"withdrawId\":\"7667\""));
        assertTrue(capturedBody.get().contains("\"password\":\"88422\""));
        assertTrue(capturedBody.get().contains("\"shop\":\"998739\""));
        assertTrue(capturedBody.get().contains("\"token\":\"token-test\""));
        assertEquals("998765", String.valueOf(response.getPayload().get("userid")));
        assertEquals("USD", String.valueOf(response.getPayload().get("currency")));
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
        provider.setCountry(66);
        provider.setServiceProviderCode("1");

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().getProviders().put("ecuabet", provider);
        return appProperties;
    }
}

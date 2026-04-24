package com.omnistack.backend.infrastructure.adapter.integration.loteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.domain.model.ProviderTokenLoginCommand;
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

class LoteriaTokenLoginWebClientAdapterTest {

    private final AtomicReference<String> capturedPath = new AtomicReference<>();
    private final AtomicReference<String> capturedBody = new AtomicReference<>();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldRequestTokenUsingConfiguredLoginPayload() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/Login", exchange -> respondJson(exchange,
                """
                {
                  "usuario":"USRFEMSAPREP",
                  "token":"43888023032026110446438880234611",
                  "codError":0,
                  "msgError":""
                }
                """));
        server.start();

        LoteriaTokenLoginWebClientAdapter adapter = new LoteriaTokenLoginWebClientAdapter(
                WebClient.builder().build(),
                new ObjectMapper());

        var response = adapter.login(ProviderTokenLoginCommand.builder()
                .providerName("LOTERIA NACIONAL")
                .serviceProviderCode("2")
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .path("/APIVentasLoteria/api/Ventas/Login")
                .username("USRFEMSAPREP")
                .password("F3m993sA.")
                .productToSell("Bet593")
                .build());

        assertEquals("/APIVentasLoteria/api/Ventas/Login", capturedPath.get());
        assertTrue(capturedBody.get().contains("\"username\":\"USRFEMSAPREP\""));
        assertTrue(capturedBody.get().contains("\"password\":\"F3m993sA.\""));
        assertTrue(capturedBody.get().contains("\"productoVender\":\"Bet593\""));
        assertEquals("43888023032026110446438880234611", response.getToken());
    }

    private void respondJson(HttpExchange exchange, String body) throws IOException {
        respond(exchange, body, "text/plain; charset=utf-8");
    }

    private void respond(HttpExchange exchange, String body, String contentType) throws IOException {
        capturedPath.set(exchange.getRequestURI().getPath());
        capturedBody.set(readBody(exchange.getRequestBody()));

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String readBody(InputStream bodyStream) throws IOException {
        return new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}

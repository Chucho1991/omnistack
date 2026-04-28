package com.omnistack.backend.infrastructure.adapter.integration.loteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.Bet593RechargeCommand;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

class Bet593RechargeWebClientAdapterTest {

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
    void shouldSendBet593RechargePayloadAndNormalizeSuccessfulResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/RecargarBet593", exchange -> respondJson(exchange,
                """
                {
                  "usuario": "usrfemsaprep",
                  "token": "token-dinamico",
                  "operacion": "RECARGA593",
                  "codError": 0,
                  "msgError": "",
                  "resultado": "0",
                  "cuentaweb": "0901111112",
                  "nombre": "Usuario",
                  "apellido": "Prueba uno",
                  "tipoDocumento": "CI",
                  "valor": "9.99",
                  "fecharecarga": "2026-04-24T11:17:17",
                  "recargaid": "4E26639D-DB2E-4E07-90E0-7C2B16DDA5FE",
                  "serialnumber": "serial-1",
                  "estado": null
                }
                """));
        server.start();

        Bet593RechargeWebClientAdapter adapter = new Bet593RechargeWebClientAdapter(
                WebClient.builder().build(),
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-dinamico");

        var response = adapter.recharge(Bet593RechargeCommand.builder()
                .uuid("uuid-bet593")
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .authorization("9F968187-F436-4F19-8C1F-A7A4DA07A899")
                .serialnumber("7366ea56284a06a2")
                .document("0901111112")
                .amount(new BigDecimal("9.99"))
                .build(), "/APIVentasLoteria/api/Ventas/RecargarBet593");

        assertEquals("/APIVentasLoteria/api/Ventas/RecargarBet593", capturedPath.get());
        assertTrue(capturedBody.get().contains("\"usuario\":\"USRFEMSAPREP\""));
        assertTrue(capturedBody.get().contains("\"token\":\"token-dinamico\""));
        assertTrue(capturedBody.get().contains("\"canal\":\"BMV\""));
        assertTrue(capturedBody.get().contains("\"medioId\":23"));
        assertTrue(capturedBody.get().contains("\"puntooperacionId\":52132"));
        assertTrue(capturedBody.get().contains("\"cuentaweb\":\"0901111112\""));
        assertTrue(capturedBody.get().contains("\"recargaid\":\"9F968187-F436-4F19-8C1F-A7A4DA07A899\""));
        assertTrue(capturedBody.get().contains("\"serialnumber\":\"7366ea56284a06a2\""));
        assertTrue(capturedBody.get().contains("\"valor\":\"9.99\""));
        assertTrue(capturedBody.get().contains("\"codigotrn\":\"uuid-bet593\""));
        assertTrue(response.isApproved());
        assertEquals("0", response.getExternalCode());
        assertEquals("Usuario", String.valueOf(response.getPayload().get("name")));
        assertEquals("Prueba uno", String.valueOf(response.getPayload().get("lastname")));
        assertEquals("4E26639D-DB2E-4E07-90E0-7C2B16DDA5FE", String.valueOf(response.getPayload().get("authorization")));
    }

    @Test
    void shouldSendBet593ValidationPayloadAndNormalizeCommitResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/ValidarBet593", exchange -> respondJson(exchange,
                """
                {
                  "usuario": "usrfemsaprep",
                  "token": "token-dinamico",
                  "operacion": "VALIDA593",
                  "codError": 0,
                  "msgError": "",
                  "resultado": "0",
                  "cuentaweb": "0901111112",
                  "nombre": null,
                  "apellido": null,
                  "tipoDocumento": null,
                  "valor": null,
                  "fecharecarga": null,
                  "recargaid": null,
                  "serialnumber": null,
                  "estado": "COMMIT"
                }
                """));
        server.start();

        Bet593RechargeWebClientAdapter adapter = new Bet593RechargeWebClientAdapter(
                WebClient.builder().build(),
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-dinamico");

        var response = adapter.validateRecharge(Bet593RechargeCommand.builder()
                .uuid("uuid-bet593")
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .authorization("9F968187-F436-4F19-8C1F-A7A4DA07A899")
                .serialnumber("7366ea56284a06a2")
                .document("0901111112")
                .build(), "/APIVentasLoteria/api/Ventas/ValidarBet593");

        assertEquals("/APIVentasLoteria/api/Ventas/ValidarBet593", capturedPath.get());
        assertTrue(capturedBody.get().contains("\"usuario\":\"USRFEMSAPREP\""));
        assertTrue(capturedBody.get().contains("\"token\":\"token-dinamico\""));
        assertTrue(capturedBody.get().contains("\"canal\":\"BMV\""));
        assertTrue(capturedBody.get().contains("\"medioId\":23"));
        assertTrue(capturedBody.get().contains("\"puntooperacionId\":52132"));
        assertTrue(capturedBody.get().contains("\"cuentaweb\":\"0901111112\""));
        assertTrue(capturedBody.get().contains("\"recargaid\":\"9F968187-F436-4F19-8C1F-A7A4DA07A899\""));
        assertTrue(capturedBody.get().contains("\"serialnumber\":\"7366ea56284a06a2\""));
        assertFalse(capturedBody.get().contains("\"valor\""));
        assertFalse(capturedBody.get().contains("\"codigotrn\""));
        assertTrue(response.isApproved());
        assertEquals("0", response.getExternalCode());
        assertEquals("0901111112", String.valueOf(response.getPayload().get("document")));
        assertEquals("COMMIT", String.valueOf(response.getPayload().get("status")));
    }

    @Test
    void shouldConvertReadTimeoutIntoIntegrationException() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/RecargarBet593", exchange -> {
            try {
                Thread.sleep(200);
                respondJson(exchange, "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        server.start();

        WebClient timeoutWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofMillis(20))))
                .build();
        Bet593RechargeWebClientAdapter adapter = new Bet593RechargeWebClientAdapter(
                timeoutWebClient,
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-dinamico");

        IntegrationException exception = assertThrows(IntegrationException.class, () -> adapter.recharge(
                Bet593RechargeCommand.builder()
                        .uuid("uuid-bet593")
                        .categoryCode("1")
                        .subcategoryCode("1")
                        .serviceProviderCode("2")
                        .document("0901111112")
                        .amount(new BigDecimal("9.99"))
                        .build(),
                "/APIVentasLoteria/api/Ventas/RecargarBet593"));

        assertTrue(exception.getMessage().contains("Timeout al invocar recarga BET593"));
    }

    private void respondJson(HttpExchange exchange, String body) throws IOException {
        capturedPath.set(exchange.getRequestURI().getPath());
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
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");
        provider.setCanal("BMV");
        provider.setMedioId(23);
        provider.setPuntoOperacionId(52132);
        provider.getAuth().getLogin().setUsername("USRFEMSAPREP");

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().getProviders().put("loteria", provider);
        return appProperties;
    }
}

package com.omnistack.backend.infrastructure.adapter.integration.loteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.Bet593WithdrawCommand;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

class Bet593WithdrawWebClientAdapterTest {

    private static final String GENERATED_UUID = "f0908f64-9145-45cf-a22c-c36bca604372";

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
    void shouldSendBet593WithdrawPayloadAndNormalizeSuccessfulResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/RetirarBet593", exchange -> respondJson(exchange,
                """
                {
                  "codError": 0,
                  "msgError": "",
                  "usuario": "USRFEMSAPREP",
                  "operacion": "RETIROOL",
                  "token": "token-dinamico",
                  "ordenPagoId": 71787,
                  "identificacion": "0911274165",
                  "valor": "17.000000",
                  "numeroTransaccion": "f0908f64-9145-45cf-a22c-c36bca604372",
                  "nombre": null,
                  "fecha": "2026-04-27T18:53:00"
                }
                """));
        server.start();

        Bet593WithdrawWebClientAdapter adapter = new Bet593WithdrawWebClientAdapter(
                WebClient.builder().build(),
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-dinamico");

        var response = adapter.withdraw(Bet593WithdrawCommand.builder()
                .uuid(GENERATED_UUID)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .document("0911274165")
                .withdrawId("20240430800100007")
                .build(), "/APIVentasLoteria/api/Ventas/RetirarBet593");

        assertEquals("/APIVentasLoteria/api/Ventas/RetirarBet593", capturedPath.get());
        assertTrue(capturedBody.get().contains("\"usuario\":\"USRFEMSAPREP\""));
        assertTrue(capturedBody.get().contains("\"maquina\":\"192.168.3.230\""));
        assertTrue(capturedBody.get().contains("\"operacion\":\"RETIROOL\""));
        assertTrue(capturedBody.get().contains("\"token\":\"token-dinamico\""));
        assertTrue(capturedBody.get().contains("\"usuarioId\":\"USRFEMSAPREP\""));
        assertTrue(capturedBody.get().contains("\"clienteId\":58542"));
        assertTrue(capturedBody.get().contains("\"medioId\":23"));
        assertTrue(capturedBody.get().contains("\"numeroTransaccion\":\"" + GENERATED_UUID + "\""));
        assertTrue(capturedBody.get().contains("\"identificacion\":\"0911274165\""));
        assertTrue(capturedBody.get().contains("\"numeroRetiro\":\"20240430800100007\""));
        assertTrue(response.isApproved());
        assertEquals("0", response.getExternalCode());
        assertEquals("71787", String.valueOf(response.getPayload().get("authorization")));
        assertEquals("0911274165", String.valueOf(response.getPayload().get("document")));
        assertEquals("17.000000", String.valueOf(response.getPayload().get("amount")));
        assertEquals(GENERATED_UUID, String.valueOf(response.getPayload().get("transactionNumber")));
    }

    @Test
    void shouldKeepRequestUuidWhenProviderDoesNotEchoTransactionNumber() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/RetirarBet593", exchange -> respondJson(exchange,
                """
                {
                  "codError": 400022,
                  "msgError": "Orden de pago no disponible para pago o ya esta pagada,",
                  "usuario": "",
                  "operacion": "",
                  "token": "token-dinamico",
                  "ordenPagoId": null,
                  "identificacion": null,
                  "valor": null,
                  "numeroTransaccion": null,
                  "nombre": null,
                  "fecha": "0001-01-01T00:00:00"
                }
                """));
        server.start();

        Bet593WithdrawWebClientAdapter adapter = new Bet593WithdrawWebClientAdapter(
                WebClient.builder().build(),
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-dinamico");

        var response = adapter.withdraw(Bet593WithdrawCommand.builder()
                .uuid(GENERATED_UUID)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .document("0911274165")
                .withdrawId("20240430800100007")
                .build(), "/APIVentasLoteria/api/Ventas/RetirarBet593");

        assertTrue(capturedBody.get().contains("\"numeroTransaccion\":\"" + GENERATED_UUID + "\""));
        assertEquals("400022", response.getExternalCode());
        assertEquals(GENERATED_UUID, String.valueOf(response.getPayload().get("transactionNumber")));
    }

    @Test
    void shouldSendBet593WithdrawValidationPayload() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/ConsultarRetiroBet593", exchange -> respondJson(exchange,
                """
                {
                  "codError": 400022,
                  "msgError": "Orden de pago no disponible para pago o ya esta pagada,",
                  "usuario": "",
                  "operacion": "",
                  "token": "token-dinamico",
                  "ordenPagoId": null,
                  "identificacion": null,
                  "valor": null,
                  "numeroTransaccion": null,
                  "nombre": null,
                  "fecha": "0001-01-01T00:00:00"
                }
                """));
        server.start();

        Bet593WithdrawWebClientAdapter adapter = new Bet593WithdrawWebClientAdapter(
                WebClient.builder().build(),
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-dinamico");

        var response = adapter.validateWithdraw(Bet593WithdrawCommand.builder()
                .uuid(GENERATED_UUID)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .document("0901111112")
                .withdrawId("340468406359")
                .build(), "/APIVentasLoteria/api/Ventas/ConsultarRetiroBet593");

        assertEquals("/APIVentasLoteria/api/Ventas/ConsultarRetiroBet593", capturedPath.get());
        assertTrue(capturedBody.get().contains("\"operacion\":\"CONRETIROOL\""));
        assertTrue(capturedBody.get().contains("\"numeroRetiro\":\"340468406359\""));
        assertTrue(capturedBody.get().contains("\"identificacion\":\"0901111112\""));
        assertEquals("400022", response.getExternalCode());
        assertEquals(GENERATED_UUID, String.valueOf(response.getPayload().get("transactionNumber")));
    }

    @Test
    void shouldSendBet593WithdrawReversePayload() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/ReversarRetiroBet593", exchange -> respondJson(exchange,
                """
                {
                  "codError": 0,
                  "msgError": "",
                  "usuario": "USRFEMSAPREP",
                  "operacion": "REVRETIROOL",
                  "token": "token-dinamico",
                  "ordenPagoId": 82319,
                  "identificacion": "0901111112",
                  "valor": "200.000000",
                  "numeroTransaccion": "ca9b201a-a668-45ed-876c-00affcb18580",
                  "nombre": null,
                  "fecha": "2026-04-27T22:42:00"
                }
                """));
        server.start();

        Bet593WithdrawWebClientAdapter adapter = new Bet593WithdrawWebClientAdapter(
                WebClient.builder().build(),
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-dinamico");

        var response = adapter.reverseWithdraw(Bet593WithdrawCommand.builder()
                .uuid(GENERATED_UUID)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .authorization("ca9b201a-a668-45ed-876c-00affcb18580")
                .document("0901111112")
                .motivo("Demora en obtener respuesta")
                .build(), "/APIVentasLoteria/api/Ventas/ReversarRetiroBet593");

        assertEquals("/APIVentasLoteria/api/Ventas/ReversarRetiroBet593", capturedPath.get());
        assertTrue(capturedBody.get().contains("\"operacion\":\"REVRETIROOL\""));
        assertTrue(capturedBody.get().contains("\"numeroTransaccion\":\"ca9b201a-a668-45ed-876c-00affcb18580\""));
        assertTrue(capturedBody.get().contains("\"identificacion\":\"0901111112\""));
        assertTrue(capturedBody.get().contains("\"motivo\":\"Demora en obtener respuesta\""));
        assertTrue(response.isApproved());
        assertEquals("0", response.getExternalCode());
        assertEquals("0901111112", String.valueOf(response.getPayload().get("document")));
        assertEquals("ca9b201a-a668-45ed-876c-00affcb18580", String.valueOf(response.getPayload().get("transactionNumber")));
    }

    @Test
    void shouldConvertReadTimeoutIntoIntegrationException() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/APIVentasLoteria/api/Ventas/RetirarBet593", exchange -> {
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
        Bet593WithdrawWebClientAdapter adapter = new Bet593WithdrawWebClientAdapter(
                timeoutWebClient,
                appProperties("http://localhost:" + server.getAddress().getPort()),
                new ObjectMapper(),
                (categoryCode, subcategoryCode, serviceProviderCode) -> "token-dinamico");

        IntegrationException exception = assertThrows(IntegrationException.class, () -> adapter.withdraw(
                Bet593WithdrawCommand.builder()
                        .uuid(GENERATED_UUID)
                        .categoryCode("1")
                        .subcategoryCode("1")
                        .serviceProviderCode("2")
                        .document("0911274165")
                        .withdrawId("20240430800100007")
                        .build(),
                "/APIVentasLoteria/api/Ventas/RetirarBet593"));

        assertTrue(exception.getMessage().contains("Timeout al invocar nota de retiro BET593"));
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
        provider.setShopIp("192.168.3.230");
        provider.setClienteId(58542);
        provider.setMedioId(23);
        provider.getAuth().getLogin().setUsername("USRFEMSAPREP");
        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashout().setName("RETIROOL");
        provider.getServices().put("EXECUTE", capabilityProperties);
        AppProperties.ProviderCapabilityProperties verifyCapabilityProperties = new AppProperties.ProviderCapabilityProperties();
        verifyCapabilityProperties.getCashout().setName("CONRETIROOL");
        provider.getServices().put("VERIFY", verifyCapabilityProperties);
        AppProperties.ProviderCapabilityProperties reverseCapabilityProperties = new AppProperties.ProviderCapabilityProperties();
        reverseCapabilityProperties.getCashout().setName("REVRETIROOL");
        provider.getServices().put("REVERSE", reverseCapabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().getProviders().put("loteria", provider);
        return appProperties;
    }
}

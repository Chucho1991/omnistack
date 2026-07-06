package com.omnistack.backend.infrastructure.adapter.integration.ecuabet;

import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.WsExtLogService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetUserSearchCommand;
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
import org.mockito.Mockito;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

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
                providerConfigService(),
                new ObjectMapper(),
                new ProviderTokenResolverUseCase() {
                    public String getToken(String categoryCode, String subcategoryCode, String serviceProviderCode) { return "token-test"; }
                    public String getToken(String providerKey) { return "token-test"; }
                }, Mockito.mock(WsExtLogService.class));

        var response = adapter.searchUser(EcuabetUserSearchCommand.builder()
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .withdrawId("7667")
                .password("88422")
                .build(), "http://localhost:" + server.getAddress().getPort() + "/user/searchwithdraw");

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

    @Test
    void shouldRejectBusinessErrorResponseWithoutErrorField() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/user/search", exchange -> respondJson(exchange,
                """
                {
                  "code": "101",
                  "message": "Usuario invalido"
                }
                """));
        server.start();

        EcuabetUserSearchWebClientAdapter adapter = new EcuabetUserSearchWebClientAdapter(
                WebClient.builder().build(),
                providerConfigService(),
                new ObjectMapper(),
                new ProviderTokenResolverUseCase() {
                    public String getToken(String categoryCode, String subcategoryCode, String serviceProviderCode) { return "token-test"; }
                    public String getToken(String providerKey) { return "token-test"; }
                }, Mockito.mock(WsExtLogService.class));

        var response = adapter.searchUser(EcuabetUserSearchCommand.builder()
                .chain("60")
                .store("4")
                .storeName("Local 4")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .document("2912912912")
                .build(), "http://localhost:" + server.getAddress().getPort() + "/user/search");

        assertFalse(response.isApproved());
        assertEquals("02", response.getExternalCode());
        assertEquals("Usuario invalido", response.getExternalMessage());
    }

    @Test
    void shouldMapBusinessErrorCodeFromErrorFieldAndMessageFromMessageField() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/user/search", exchange -> respondJson(exchange,
                """
                {
                  "code": "101",
                  "error": 1,
                  "name": null,
                  "userid": null,
                  "raw": {},
                  "message": "Usuario no encontrado"
                }
                """));
        server.start();

        EcuabetUserSearchWebClientAdapter adapter = new EcuabetUserSearchWebClientAdapter(
                WebClient.builder().build(),
                providerConfigService(),
                new ObjectMapper(),
                new ProviderTokenResolverUseCase() {
                    public String getToken(String categoryCode, String subcategoryCode, String serviceProviderCode) { return "token-test"; }
                    public String getToken(String providerKey) { return "token-test"; }
                }, Mockito.mock(WsExtLogService.class));

        var response = adapter.searchUser(EcuabetUserSearchCommand.builder()
                .chain("60")
                .store("4")
                .storeName("Local 4")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .document("2912912912")
                .build(), "http://localhost:" + server.getAddress().getPort() + "/user/search");

        assertFalse(response.isApproved());
        assertEquals("02", response.getExternalCode());
        assertEquals("Usuario no encontrado", response.getExternalMessage());
        assertEquals("Usuario no encontrado", response.getPayload().get("message"));
    }

    @Test
    void shouldRejectUppercaseResponseWithoutLookupData() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/user/search-1", exchange -> respondJson(exchange,
                """
                {
                  "Error": false,
                  "Code": "0",
                  "message": null
                }
                """));
        server.start();

        EcuabetUserSearchWebClientAdapter adapter = new EcuabetUserSearchWebClientAdapter(
                WebClient.builder().build(),
                providerConfigService(),
                new ObjectMapper(),
                new ProviderTokenResolverUseCase() {
                    public String getToken(String categoryCode, String subcategoryCode, String serviceProviderCode) { return "token-test"; }
                    public String getToken(String providerKey) { return "token-test"; }
                }, Mockito.mock(WsExtLogService.class));

        var response = adapter.searchUser(EcuabetUserSearchCommand.builder()
                .chain("60")
                .store("4")
                .storeName("Local 4")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .document("2912912912")
                .build(), "http://localhost:" + server.getAddress().getPort() + "/user/search-1");

        assertFalse(response.isApproved());
        assertEquals("0", response.getExternalCode());
        assertEquals("ECUABET no retorno datos para aprobar el precheck", response.getExternalMessage());
        assertEquals(0, response.getPayload().get("error"));
    }

    @Test
    void shouldConvertTransportErrorIntoIntegrationException() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/user/search", exchange -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        server.start();

        WebClient timeoutWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofMillis(30))))
                .build();
        EcuabetUserSearchWebClientAdapter adapter = new EcuabetUserSearchWebClientAdapter(
                timeoutWebClient,
                providerConfigService(),
                new ObjectMapper(),
                new ProviderTokenResolverUseCase() {
                    public String getToken(String categoryCode, String subcategoryCode, String serviceProviderCode) { return "token-test"; }
                    public String getToken(String providerKey) { return "token-test"; }
                },
                Mockito.mock(WsExtLogService.class));

        IntegrationException exception = assertThrows(IntegrationException.class, () -> adapter.searchUser(
                EcuabetUserSearchCommand.builder()
                        .chain("60")
                        .store("4")
                        .storeName("Local 4")
                        .pos("1")
                        .channelPos(ChannelPos.POS)
                        .movementType(MovementType.CASH_IN)
                        .categoryCode("1")
                        .subcategoryCode("1")
                        .document("2912912912")
                        .build(),
                "/user/search"));

        assertTrue(exception.getMessage().contains("Error de transporte al invocar ECUABET"));
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

    private ProviderConfigService providerConfigService() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setShopId("998739");
        provider.setCountry(66);
        provider.setServiceProviderCode("1");

        ProviderConfigService mock = Mockito.mock(ProviderConfigService.class);
        Mockito.when(mock.getProviderProperties("ecuabet")).thenReturn(provider);
        return mock;
    }
}

package com.omnistack.backend.infrastructure.adapter.integration.claro;

import com.omnistack.backend.application.port.out.ClaroExecutePort;
import com.omnistack.backend.application.port.out.ClaroPrecheckPort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.WsExtLogService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.ClaroExecuteCommand;
import com.omnistack.backend.domain.model.ClaroPrecheckCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ProviderCallLog;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import reactor.core.publisher.Mono;

/**
 * Adapter XML para las operaciones PRECHECK y EXECUTE del proveedor CLARO.
 * Construye y parsea el formato umsprot (no es SOAP estandar).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaroXmlAdapter implements ClaroPrecheckPort, ClaroExecutePort {

    private static final String PROVIDER_KEY = "claro";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String WS_KEY_PRECHECK = "PRECHECK.CASHIN";
    private static final String WS_KEY_EXECUTE = "EXECUTE.CASHIN";

    private final WebClient omnistackWebClient;
    private final ProviderConfigService providerConfigService;
    private final WsExtLogService wsExtLogService;

    @Override
    public ExternalTransactionResponse validateRecharge(ClaroPrecheckCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String trxDate = LocalDateTime.now().format(DATE_FMT);
        String subscriberId = resolveSubscriberId(command.getPhone());

        String xmlBody = buildPrecheckXml(command, provider, trxDate, subscriberId);
        String responseXml = invokeXml(operationPath, provider, xmlBody, "validateRecharge", "validateRechargeRetail",
                command.getUuid(), WS_KEY_PRECHECK);

        return parseResponse(responseXml, "validateRechargeRetail", command.getAmount(), command.getPhone());
    }

    @Override
    public ExternalTransactionResponse processRecharge(ClaroExecuteCommand command, String operationPath) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        String trxDate = LocalDateTime.now().format(DATE_FMT);
        String subscriberId = resolveSubscriberId(command.getPhone());

        String xmlBody = buildExecuteXml(command, provider, trxDate, subscriberId);
        String responseXml = invokeXml(operationPath, provider, xmlBody, "processRecharge", "processRechargeRetail",
                command.getUuid(), WS_KEY_EXECUTE);

        return parseResponse(responseXml, "processRechargeRetail", command.getAmount(), command.getPhone());
    }

    private String buildPrecheckXml(
            ClaroPrecheckCommand command,
            AppProperties.ProviderProperties provider,
            String trxDate,
            String subscriberId) {
        return "<umsprot version=\"1\">"
                + "<exec_req function=\"validateRechargeRetail\">"
                + field("validateRechargeRetail", "COMPANYID", provider.getCompanyId())
                + field("validateRechargeRetail", "EXTERNALOPERATION", provider.getExternalOperation())
                + field("validateRechargeRetail", "EXTERNALTRANSACTIONDATE", trxDate)
                + field("validateRechargeRetail", "USERNAME", provider.getAuth().getLogin().getUsername())
                + field("validateRechargeRetail", "PASSWORD", provider.getAuth().getLogin().getPassword())
                + field("validateRechargeRetail", "MEDIAID", provider.getMediaId())
                + field("validateRechargeRetail", "TERMINAL", provider.getShopIp())
                + field("validateRechargeRetail", "CODCAJA", provider.getCodCaja())
                + field("validateRechargeRetail", "CODSITE", provider.getCodSite())
                + field("validateRechargeRetail", "SUBSCRIBERID", subscriberId)
                + field("validateRechargeRetail", "QUANTITY", command.getAmount())
                + field("validateRechargeRetail", "OFFERID", command.getOfferId())
                + field("validateRechargeRetail", "EXTERNALTRANSACTIONID", command.getUuid())
                + "</exec_req>"
                + "</umsprot>";
    }

    private String buildExecuteXml(
            ClaroExecuteCommand command,
            AppProperties.ProviderProperties provider,
            String trxDate,
            String subscriberId) {
        return "<umsprot version=\"1\">"
                + "<exec_req function=\"processRechargeRetail\">"
                + field("processRechargeRetail", "COMPANYID", provider.getCompanyId())
                + field("processRechargeRetail", "CONSUMERID", provider.getConsumerId())
                + field("processRechargeRetail", "EXTERNALOPERATION", provider.getExternalOperation())
                + field("processRechargeRetail", "EXTERNALTRANSACTIONDATE", trxDate)
                + field("processRechargeRetail", "CHANNELID", provider.getChannelId())
                + field("processRechargeRetail", "MEDIAID", provider.getMediaId())
                + field("processRechargeRetail", "MEDIADETAILID", provider.getMediaDetailId())
                + field("processRechargeRetail", "USERNAME", provider.getAuth().getLogin().getUsername())
                + field("processRechargeRetail", "PASSWORD", provider.getAuth().getLogin().getPassword())
                + field("processRechargeRetail", "TERMINAL", provider.getShopIp())
                + field("processRechargeRetail", "CODCAJA", provider.getCodCaja())
                + field("processRechargeRetail", "CODSITE", provider.getCodSite())
                + field("processRechargeRetail", "SUBSCRIBERID", subscriberId)
                + field("processRechargeRetail", "SUBSCRIBERTYPE", provider.getSubscriberType())
                + field("processRechargeRetail", "SUBSCRIPTIONTYPE", provider.getSubscriptionType())
                + field("processRechargeRetail", "QUANTITY", command.getAmount())
                + field("processRechargeRetail", "OFFERID", command.getOfferId())
                + field("processRechargeRetail", "AUTHORIZATIONNUMBER", command.getAuthorizationNumber())
                + field("processRechargeRetail", "EXTERNALTRANSACTIONID", command.getUuid())
                + field("processRechargeRetail", "TOKEN", provider.getToken())
                + field("processRechargeRetail", "LATITUDE", provider.getLatitude())
                + field("processRechargeRetail", "LONGITUDE", provider.getLongitude())
                + field("processRechargeRetail", "CANTON", provider.getCanton())
                + field("processRechargeRetail", "PROVINCE", provider.getProvince())
                + field("processRechargeRetail", "PARISH", provider.getParish())
                + "</exec_req>"
                + "</umsprot>";
    }

    private ExternalTransactionResponse parseResponse(
            String responseXml,
            String elementName,
            String requestAmount,
            String phone) {
        Map<String, String> fields;
        try {
            fields = parseXmlFields(responseXml, elementName);
        } catch (Exception e) {
            throw new IntegrationException("CLARO retorno XML no parseable: " + e.getMessage(), e);
        }

        String idCode = fields.getOrDefault("ID_CODE", "");
        String status = fields.getOrDefault("STATUS", "");
        String systemMessage = fields.getOrDefault("SYSTEMMESSAGE", "");
        String authorizationNumber = fields.getOrDefault("AUTHORIZATIONNUMBER", "");

        boolean isError = !"0".equals(idCode);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("authorization", authorizationNumber);
        payload.put("amount", requestAmount);
        payload.put("phone", phone);
        payload.put("id_code", idCode);
        payload.put("status", status);
        payload.put("system_message", systemMessage);

        return ExternalTransactionResponse.builder()
                .approved(!isError)
                .externalCode(idCode)
                .externalMessage(systemMessage)
                .payload(payload)
                .build();
    }

    private Map<String, String> parseXmlFields(String xml, String elementName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        Map<String, String> result = new LinkedHashMap<>();
        NodeList nodes = doc.getElementsByTagName(elementName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String name = el.getAttribute("name");
            String value = el.getTextContent();
            if (name != null && !name.isBlank()) {
                result.put(name, value);
            }
        }
        return result;
    }

    private String invokeXml(
            String operationPath,
            AppProperties.ProviderProperties provider,
            String xmlBody,
            String logOperation,
            String errorOperation,
            String uuid,
            String wsKey) {
        String url = operationPath;
        long startMs = System.currentTimeMillis();
        log.info("CLARO {} request url={} body={}", logOperation, url, xmlBody);

        String responseBody;
        try {
            responseBody = omnistackWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_XML)
                    .bodyValue(xmlBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                log.error("CLARO {} error url={} body={}", logOperation, url, body);
                                String errMsg = "Error HTTP al invocar " + errorOperation + ": " + body;
                                wsExtLogService.log(ProviderCallLog.builder()
                                        .uuid(uuid)
                                        .providerKey(PROVIDER_KEY)
                                        .wsKey(wsKey)
                                        .url(url)
                                        .requestJson(xmlBody)
                                        .responseJson(body)
                                        .durationMs(System.currentTimeMillis() - startMs)
                                        .isError(true)
                                        .errorMessage(errMsg)
                                        .build());
                                return Mono.error(new IntegrationException(errMsg));
                            }))
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientRequestException exception) {
            String errMsg = "Error de conexion al invocar " + errorOperation + ": " + rootCauseMessage(exception);
            wsExtLogService.log(ProviderCallLog.builder()
                    .uuid(uuid)
                    .providerKey(PROVIDER_KEY)
                    .wsKey(wsKey)
                    .url(url)
                    .requestJson(xmlBody)
                    .responseJson(null)
                    .durationMs(System.currentTimeMillis() - startMs)
                    .isError(true)
                    .errorMessage(errMsg)
                    .build());
            throw new IntegrationException(errMsg, exception);
        }

        if (responseBody == null || responseBody.isBlank()) {
            throw new IntegrationException("CLARO no retorno contenido para " + errorOperation);
        }
        log.info("CLARO {} response url={} body={}", logOperation, url, responseBody);
        wsExtLogService.log(ProviderCallLog.builder()
                .uuid(uuid)
                .providerKey(PROVIDER_KEY)
                .wsKey(wsKey)
                .url(url)
                .requestJson(xmlBody)
                .responseJson(responseBody)
                .durationMs(System.currentTimeMillis() - startMs)
                .isError(false)
                .build());
        return responseBody;
    }

    private static String field(String elementName, String fieldName, String value) {
        String v = value != null ? value : "";
        return "<" + elementName + " name=\"" + fieldName + "\">" + v + "</" + elementName + ">";
    }

    private static String resolveSubscriberId(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String digits = phone.replaceFirst("^0", "");
        return "593" + digits;
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(PROVIDER_KEY);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor CLARO");
        }
        return provider;
    }


    private String rootCauseMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : current.getClass().getSimpleName() + ": " + message;
    }
}

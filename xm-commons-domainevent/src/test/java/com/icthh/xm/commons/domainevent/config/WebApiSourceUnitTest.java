package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.domain.DomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.HttpDomainEventPayload;
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource;
import com.icthh.xm.commons.domainevent.service.EventPublisher;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.net.http.HttpHeaders;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiMaskConfig.class)
@EnableConfigurationProperties
public class WebApiSourceUnitTest {

    private static final String CONTENT = "{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\",\"content\":{\"value\":\"someValue\",\"text\":\"someText\"}}";
    private static final String MASKED_CONTENT = "{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\",\"content\":{\"value\":\"mask\",\"text\":\"mask\"}}";
    private static final String TENANT = "test";

    @Autowired
    private ApiMaskConfig apiMaskConfig;

    @Mock
    private XmDomainEventConfiguration xmDomainEventConfiguration;

    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;
    @Mock
    private ContentCachingRequestWrapper request;
    @Mock
    private ContentCachingResponseWrapper response;

    private WebApiSource webApiSource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        webApiSource = new WebApiSource(eventPublisher, xmAuthenticationContextHolder, apiMaskConfig, xmDomainEventConfiguration);

        when(request.getContentAsByteArray()).thenReturn(CONTENT.getBytes());
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("Authorization", "Domain", "x-tenant")));
        when(request.getHeaders("Domain")).thenReturn(Collections.enumeration(Collections.singletonList("test")));
        when(request.getHeaders("x-tenant")).thenReturn(Collections.enumeration(Collections.singletonList(TENANT)));
        when(request.getHeader("x-tenant")).thenReturn(TENANT);

        when(response.getContentAsByteArray()).thenReturn(MASKED_CONTENT.getBytes());
        when(response.getStatus()).thenReturn(200);
        when(response.getHeaderNames()).thenReturn(Collections.singletonList("Domain"));
        when(response.getHeader("Domain")).thenReturn("test");
    }

    @Test
    public void sendSuccessMaskRequestResponse() {
        when(request.getRequestURI()).thenReturn("/api/attachments");
        when(request.getMethod()).thenReturn("PUT");
        when(xmDomainEventConfiguration.getOperationMapping(TENANT, "PUT", "/api/attachments")).thenReturn("changed");

        DomainEvent expectedEvent = createExpectedEvent("changed");
        HttpDomainEventPayload expectedPayload = (HttpDomainEventPayload) createExpectedPayload(MASKED_CONTENT);

        DomainEvent event = webApiSource.createEvent(request, response, TENANT, null, null);

        assertDomainEvent(expectedEvent, event);

        HttpDomainEventPayload payload = (HttpDomainEventPayload) event.getPayload();
        assertPayload(expectedPayload, payload);
    }

    @Test
    public void sendSuccessMaskResponse() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/attachments/1");
        when(request.getMethod()).thenReturn("GET");
        when(xmDomainEventConfiguration.getOperationMapping(TENANT, "GET", "/api/attachments/1")).thenReturn("viewed");

        DomainEvent expectedEvent = createExpectedEvent("viewed");
        HttpDomainEventPayload expectedPayload = (HttpDomainEventPayload) createExpectedPayload(CONTENT);

        DomainEvent event = webApiSource.createEvent(request, response, TENANT, null, null);

        assertDomainEvent(expectedEvent, event);

        HttpDomainEventPayload payload = (HttpDomainEventPayload) event.getPayload();
        assertPayload(expectedPayload, payload);
    }

    private void assertPayload(HttpDomainEventPayload expectedPayload, HttpDomainEventPayload payload) {
        assertNotNull(payload);

        assertEquals(payload.getMethod(), expectedPayload.getMethod());
        assertEquals(payload.getUrl(), expectedPayload.getUrl());
        assertEquals(payload.getQueryString(), expectedPayload.getQueryString());
        assertEquals(payload.getRequestLength(), expectedPayload.getRequestLength());
        assertEquals(payload.getRequestBody(), expectedPayload.getRequestBody());
        assertEquals(payload.getResponseBody(), expectedPayload.getResponseBody());
        assertEquals(payload.getResponseLength(), expectedPayload.getResponseLength());
        assertEquals(payload.getRequestHeaders(), expectedPayload.getRequestHeaders());
        assertEquals(payload.getResponseHeaders(), expectedPayload.getResponseHeaders());
        assertEquals(payload.getExecTime(), expectedPayload.getExecTime());
    }

    private void assertDomainEvent(DomainEvent expectedEvent, DomainEvent event) {
        assertNotNull(event);
        assertEquals(event.getTxId(), expectedEvent.getTxId());
        assertEquals(event.getAggregateId(), expectedEvent.getAggregateId());
        assertEquals(event.getAggregateType(), expectedEvent.getAggregateType());
        assertEquals(event.getOperation(), expectedEvent.getOperation());
        assertEquals(event.getMsName(), expectedEvent.getMsName());
        assertEquals(event.getSource(), expectedEvent.getSource());
        assertEquals(event.getUserKey(), expectedEvent.getUserKey());
        assertEquals(event.getClientId(), expectedEvent.getClientId());
        assertEquals(event.getTenant(), expectedEvent.getTenant());
    }

    private DomainEvent createExpectedEvent(String operation) {
        return DomainEvent.builder()
            .id(UUID.randomUUID())
            .txId(MdcUtils.getRid())
            .aggregateId("123")
            .aggregateName("TEST_NAME")
            .aggregateType("TEST_TYPE_KEY")
            .operation(operation)
            .msName(null)
            .source(DefaultDomainEventSource.WEB.getCode())
            .userKey(null)
            .clientId(null)
            .tenant(TENANT)
            .build();
    }

    private DomainEventPayload createExpectedPayload(String requestBody) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("domain", List.of("test"));
        headers.put("x-tenant", List.of("test"));

        HttpDomainEventPayload payload = new HttpDomainEventPayload();
        payload.setMethod(request.getMethod());
        payload.setUrl(request.getRequestURI());
        payload.setQueryString(request.getQueryString());
        payload.setRequestLength(request.getContentLengthLong());
        payload.setRequestBody(requestBody);
        payload.setResponseBody(MASKED_CONTENT);
        payload.setResponseLength((long) MASKED_CONTENT.length());
        payload.setRequestHeaders(HttpHeaders.of(headers, (s1, s2) -> true));
        payload.setResponseHeaders(HttpHeaders.of(Map.of(), (s1, s2) -> true));
        payload.setResponseCode(response.getStatus());
        payload.setExecTime(MdcUtils.getExecTimeMs());

        return payload;
    }
}
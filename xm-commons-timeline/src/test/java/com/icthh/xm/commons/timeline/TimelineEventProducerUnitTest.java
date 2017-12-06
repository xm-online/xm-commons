package com.icthh.xm.commons.timeline;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.timeline.domain.ApiMaskConfig;
import com.icthh.xm.commons.timeline.domain.ApiMaskRule;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.util.Arrays;
import java.util.Collections;

public class TimelineEventProducerUnitTest {

    private static final String CONTENT = "{\"content\":{\"value\":\"someValue\",\"text\":\"someText\"}}";

    private TimelineEventProducer producer;
    @Mock
    private KafkaTemplate<Integer, String> template;
    @Mock
    private ContentCachingRequestWrapper request;
    @Mock
    private ContentCachingResponseWrapper response;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        producer = new TimelineEventProducer(template, initConfig());

        when(request.getContentAsByteArray()).thenReturn(CONTENT.getBytes());
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("Authorization", "Domain")));
        when(request.getHeaders("Domain")).thenReturn(Collections.enumeration(Collections.singletonList("test")));

        when(response.getContentAsByteArray()).thenReturn(CONTENT.getBytes());
        when(response.getStatus()).thenReturn(200);
        when(response.getHeaderNames()).thenReturn(Collections.singletonList("Domain"));
        when(response.getHeader("Domain")).thenReturn("test");
    }

    @Test
    public void sendSuccessMaskRequestResponse() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/attachments");
        when(request.getMethod()).thenReturn("PUT");

        String json = producer.createEventJson(request, response, "test", "admin", "key");
        assertEquals("admin", JsonPath.read(json, "$.login"));
        assertEquals("key", JsonPath.read(json, "$.userKey"));
        assertEquals("test", JsonPath.read(json, "$.tenant"));
        assertEquals("attachments changed", JsonPath.read(json, "$.operationName"));
        assertEquals("/api/attachments", JsonPath.read(json, "$.operationUrl"));
        assertEquals("PUT", JsonPath.read(json, "$.httpMethod"));
        assertEquals("{domain=test}", JsonPath.read(json, "$.requestHeaders").toString());
        assertEquals("{domain=test}", JsonPath.read(json, "$.responseHeaders").toString());
        assertEquals(Integer.valueOf(200), JsonPath.read(json, "$.httpStatusCode"));
        assertEquals("HTTP", JsonPath.read(json, "$.channelType"));
        assertEquals("{\"content\":{\"value\":\"mask\",\"text\":\"mask\"}}", JsonPath.read(json, "$.requestBody"));
        assertEquals("{\"content\":{\"value\":\"mask\",\"text\":\"mask\"}}", JsonPath.read(json, "$.responseBody"));
    }

    @Test
    public void sendSuccessMaskResponse() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/attachments/1");
        when(request.getMethod()).thenReturn("GET");

        String json = producer.createEventJson(request, response, "test", "admin", "key");
        assertEquals("admin", JsonPath.read(json, "$.login"));
        assertEquals("key", JsonPath.read(json, "$.userKey"));
        assertEquals("test", JsonPath.read(json, "$.tenant"));
        assertEquals("attachments viewed", JsonPath.read(json, "$.operationName"));
        assertEquals("/api/attachments/1", JsonPath.read(json, "$.operationUrl"));
        assertEquals("GET", JsonPath.read(json, "$.httpMethod"));
        assertEquals("{domain=test}", JsonPath.read(json, "$.requestHeaders").toString());
        assertEquals("{domain=test}", JsonPath.read(json, "$.responseHeaders").toString());
        assertEquals(Integer.valueOf(200), JsonPath.read(json, "$.httpStatusCode"));
        assertEquals("HTTP", JsonPath.read(json, "$.channelType"));
        assertEquals(CONTENT, JsonPath.read(json, "$.requestBody"));
        assertEquals("{\"content\":{\"value\":\"mask\",\"text\":\"someText\"}}", JsonPath.read(json, "$.responseBody"));
    }

    private ApiMaskConfig initConfig() {
        ApiMaskRule rule1 = new ApiMaskRule();
        rule1.setEndpointToMask("/api/attachments");
        rule1.setHttpMethod(Arrays.asList("POST", "PUT"));
        rule1.setPathToMask(Arrays.asList("$.content.value", "$.content.text"));
        rule1.setMask("mask");
        rule1.setMaskRequest(true);
        rule1.setMaskResponse(true);

        ApiMaskRule rule2 = new ApiMaskRule();
        rule2.setEndpointToMask("/api/attachments/**");
        rule2.setHttpMethod(Collections.singletonList("GET"));
        rule2.setPathToMask(Collections.singletonList("$.content.value"));
        rule2.setMask("mask");
        rule2.setMaskResponse(true);

        ApiMaskConfig config = new ApiMaskConfig();
        config.setMaskRules(Arrays.asList(rule1, rule2));
        return config;
    }
}

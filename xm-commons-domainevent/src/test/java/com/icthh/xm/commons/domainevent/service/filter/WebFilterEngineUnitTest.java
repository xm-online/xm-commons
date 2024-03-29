package com.icthh.xm.commons.domainevent.service.filter;

import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.config.event.InitSourceEventPublisher;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.service.filter.lep.WebLepFilter;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class WebFilterEngineUnitTest {

    public static final String TENANT = "TEST";
    public static final String UPDATE_KEY = "/config/tenants/" + TENANT + "/app-name/domainevent.yml";
    private static final String CONTENT = "{\"id\":\"123\",\"typeKey\":\"TEST_TYPE_KEY\",\"content\":{\"value\":\"someValue\",\"text\":\"someText\"}}";


    private WebFilterEngine webFilterEngine;

    private XmDomainEventConfiguration xmDomainEventConfiguration;

    @Mock
    private InitSourceEventPublisher initSourceEventPublisher;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private WebLepFilter webLepFilter;

    @Mock
    private ContentCachingRequestWrapper request;
    @Mock
    private ContentCachingResponseWrapper response;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        when(request.getContentAsByteArray()).thenReturn(CONTENT.getBytes());
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("Authorization", "Domain", "x-tenant")));
        when(request.getHeaders("Domain")).thenReturn(Collections.enumeration(Collections.singletonList("test")));
        when(request.getHeaders("x-tenant")).thenReturn(Collections.enumeration(Collections.singletonList(TENANT)));
        when(request.getHeader("x-tenant")).thenReturn(TENANT);

        when(response.getContentAsByteArray()).thenReturn(CONTENT.getBytes());
        when(response.getStatus()).thenReturn(200);
        when(response.getHeaderNames()).thenReturn(Collections.singletonList("Domain"));
        when(response.getHeader("Domain")).thenReturn("test");

        xmDomainEventConfiguration = new XmDomainEventConfiguration("app-name",
            initSourceEventPublisher,
            applicationContext);

        webFilterEngine = new WebFilterEngine(null, List.of("DELETE"), xmDomainEventConfiguration, webLepFilter);
    }

    @Test
    public void shouldNull_notConfigAndAppLevelTrue() {
        when(request.getMethod()).thenReturn("DELETE");

        DomainEvent result = webFilterEngine.isIgnoreRequest(request, response, TENANT, createSupplier(createDomainEvent()));
        assertNull(result);
    }

    @Test
    public void shouldNull_notConfigAndAppLevelFalse() {
        when(request.getMethod()).thenReturn("POST");

        DomainEvent result = webFilterEngine.isIgnoreRequest(request, response, TENANT, createSupplier(createDomainEvent()));
        assertNull(result);
    }

    @Test
    public void shouldNull_configAndAggregateTypeIn() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("app-name/api/my/full/custom/path");

        DomainEvent domainEvent = createDomainEvent();
        domainEvent.setAggregateType("CUSTOMER");
        BiFunction<String[], String, DomainEvent> supplier = createSupplier(domainEvent);

        DomainEvent result = webFilterEngine.isIgnoreRequest(request, response, TENANT, supplier);
        assertNull(result);
    }

    @Test
    public void shouldNull_configAndLepReturnFalse() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("app-name/api/my/full/custom/path");
        when(webLepFilter.lepFiltering(eq("keyName"), any(DomainEvent.class))).thenReturn(false);

        String enabledConfig = readConfigFile("/mappingDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);

        DomainEvent result = webFilterEngine.isIgnoreRequest(request, response, TENANT, createSupplier(createDomainEvent()));
        assertNull(result);
    }

    @Test
    public void shouldNull_configAndLepReturnTrue() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("app-name/api/my/full/custom/path");
        when(webLepFilter.lepFiltering(eq("keyName"), any(DomainEvent.class))).thenReturn(true);

        String enabledConfig = readConfigFile("/mappingDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);

        DomainEvent domainEvent = createDomainEvent();
        BiFunction<String[], String, DomainEvent> supplier = createSupplier(domainEvent);
        DomainEvent result = webFilterEngine.isIgnoreRequest(request, response, TENANT, supplier);
        assertEquals(result, domainEvent);
    }

    @Test
    public void shouldNull_configFilterTypeExclude() {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("app-name/api/my/delete/123");

        DomainEvent result = webFilterEngine.isIgnoreRequest(request, response, TENANT, createSupplier(createDomainEvent()));
        assertNull(result);
    }

    @Test
    public void shouldNull_configFilterTypeExcludeAndMuchByUrlAndOperationType() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("app-name/api/my/function/DICT_CITY");

        String enabledConfig = readConfigFile("/mappingDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);

        DomainEvent result = webFilterEngine.isIgnoreRequest(request, response, TENANT, createSupplier(createDomainEvent()));
        assertNull(result);
    }

    @Test
    public void shouldNull_configFilterTypeIncludeAndMuchByUrlAndOperationType() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("app-name/api/my/function/CITY");
        when(webLepFilter.lepFiltering(eq("withInclude"), any(DomainEvent.class))).thenReturn(true);

        String enabledConfig = readConfigFile("/mappingDomainEvents.yml");
        xmDomainEventConfiguration.onRefresh(UPDATE_KEY, enabledConfig);

        DomainEvent domainEvent = createDomainEvent();
        DomainEvent result = webFilterEngine.isIgnoreRequest(request, response, TENANT, createSupplier(domainEvent));
        assertEquals(result, domainEvent);
    }

    private DomainEvent createDomainEvent() {
        return DomainEvent.builder()
            .id(UUID.randomUUID())
            .tenant(TENANT)
            .build();
    }

    private BiFunction<String[], String, DomainEvent> createSupplier(DomainEvent event) {
        return (String[] aggregateDetails, String responseBody) -> event;
    }

    private String readConfigFile(String path) {
        return new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(path)))
            .lines().collect(Collectors.joining("\n"));
    }

}

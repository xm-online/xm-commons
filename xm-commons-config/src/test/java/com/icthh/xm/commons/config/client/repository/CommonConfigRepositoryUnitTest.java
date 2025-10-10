package com.icthh.xm.commons.config.client.repository;

import static com.icthh.xm.commons.config.domain.enums.ConfigEventType.UPDATE_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.message.ConfigPatternRequest;
import com.icthh.xm.commons.config.client.repository.message.GetConfigRequest;
import com.icthh.xm.commons.config.domain.ConfigQueueEvent;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.util.UriComponentsBuilder;

@RunWith(MockitoJUnitRunner.class)
public class CommonConfigRepositoryUnitTest {

    private static final String APP_NAME_TEST = "entity";

    @InjectMocks
    private XmMsConfigCommonConfigRepository configRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private XmConfigProperties xmConfigProperties;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private TenantContextHolder tenantContextHolder;

    private final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule());

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(configRepository, "allowedTenants", Set.of("XM"));
        ReflectionTestUtils.setField(configRepository, "applicationName", APP_NAME_TEST);
    }

    @Test
    public void getConfig_callsGetExchangeConfigMap() {
        configRepository = spy(new XmMsConfigCommonConfigRepository(restTemplate, xmConfigProperties, kafkaTemplate, tenantContextHolder));
        String commit = "commit";
        when(xmConfigProperties.getXmConfigUrl()).thenReturn("http://test");

        String expectedUrl = xmConfigProperties.getXmConfigUrl() + "/api/private/config_map?version=" + commit;

        Map<String, Configuration> expectedResponse = Map.of("path", new Configuration("path", "content"));
        ResponseEntity<Map<String, Configuration>> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        given(restTemplate.exchange(
                ArgumentMatchers.eq(expectedUrl),
                ArgumentMatchers.eq(HttpMethod.GET),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Configuration>>>any())
        ).willReturn(responseEntity);

        configRepository.getConfig(commit);

        verify(configRepository).getExchangeConfigMap(
                isNull(),
                eq(HttpMethod.GET),
                any(UriComponentsBuilder.class),
                any(HttpHeaders.class)
        );
    }

    @Test
    public void getConfig_withPaths_callsGetExchangeConfigMap() {
        String version = "commit";
        Collection<String> paths = List.of("path1", "path2");

        configRepository = spy(new XmMsConfigCommonConfigRepository(restTemplate, xmConfigProperties, kafkaTemplate, tenantContextHolder));
        when(xmConfigProperties.getXmConfigUrl()).thenReturn("http://test");

        String expectedUrl = xmConfigProperties.getXmConfigUrl() + "/api/private/config_map";

        Map<String, Configuration> expectedResponse = Map.of(
                "path1", new Configuration("path1", "content1"),
                "path2", new Configuration("path2", "content2")
        );
        ResponseEntity<Map<String, Configuration>> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        given(restTemplate.exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Configuration>>>any())
        ).willReturn(responseEntity);

        configRepository.getConfig(version, paths);

        verify(configRepository).getExchangeConfigMap(
                eq(new GetConfigRequest(version, paths)),
                eq(HttpMethod.POST),
                any(UriComponentsBuilder.class),
                any(HttpHeaders.class)
        );
    }

    @Test
    public void getConfigByPatternPaths_callsGetExchangeConfigMap() {
        String version = "commit";
        Collection<String> patterns = List.of("path1/*", "path2/*");

        configRepository = spy(new XmMsConfigCommonConfigRepository(restTemplate, xmConfigProperties, kafkaTemplate, tenantContextHolder));
        when(xmConfigProperties.getXmConfigUrl()).thenReturn("http://test");

        String expectedUrl = xmConfigProperties.getXmConfigUrl() + "/api/private/config_map/pattern";

        Map<String, Configuration> expectedResponse = Map.of(
                "path1/*", new Configuration("path1/*", "content1"),
                "path2/*", new Configuration("path2/*", "content2")
        );
        ResponseEntity<Map<String, Configuration>> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        given(restTemplate.exchange(
                eq(expectedUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Configuration>>>any())
        ).willReturn(responseEntity);

        configRepository.getConfigByPatternPaths(version, patterns);

        verify(configRepository).getExchangeConfigMap(
                eq(new ConfigPatternRequest(patterns, version)),
                eq(HttpMethod.POST),
                any(UriComponentsBuilder.class),
                any(HttpHeaders.class)
        );
    }

    @Test
    public void getConfig() {
        Map<String, Configuration> config = Collections
            .singletonMap("path", new Configuration("path", "content"));
        when(xmConfigProperties.getXmConfigUrl()).thenReturn("configUrl");
        when(restTemplate.exchange(eq("configUrl/api/private/config_map?version=commit"), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(config));

        assertThat(configRepository.getConfig("commit")).isEqualTo(config);
    }

    @Test
    public void updateConfig() {
        when(tenantContextHolder.getTenantKey()).thenReturn("TEST_TENANT");
        when(xmConfigProperties.getKafkaConfigQueue()).thenReturn("config_queue");

        String configPath = "/config/tenants/TEST_TENANT/service/file";
        configRepository.updateConfigFullPath(new Configuration(configPath, "content"), "hash");

        verify(tenantContextHolder).getTenantKey();
        verify(kafkaTemplate).send(
            argThat(topic -> topic.equals("config_queue")),
            argThat(isMessageWith(configPath, "content", "hash", "TEST_TENANT")));
    }

    @Test
    public void updateConfig_allowedTenant() {
        when(tenantContextHolder.getTenantKey()).thenReturn("XM");
        when(xmConfigProperties.getKafkaConfigQueue()).thenReturn("config_queue");

        String configPath = "/config/tenants/TEST_TENANT/service/file";
        configRepository.updateConfigFullPath(new Configuration(configPath, "content"), "hash");

        verify(tenantContextHolder).getTenantKey();
        verify(kafkaTemplate).send(
            argThat(topic -> topic.equals("config_queue")),
            argThat(isMessageWith(configPath, "content", "hash", "TEST_TENANT")));
    }

    @Test
    public void updateConfig_invalidTenant() {
        when(tenantContextHolder.getTenantKey()).thenReturn("INVALID_TENANT");

        String errorMessage = "Current configuration update is not allowed for tenant INVALID_TENANT. " +
            "Check your current tenant or a list of tenants with creation access.";

        assertThrows(errorMessage, BusinessException.class, () -> {
            Configuration config = new Configuration("/config/tenants/TEST_TENANT/service/file", "content");
            configRepository.updateConfigFullPath(config, "hash");
        });

        verify(tenantContextHolder).getTenantKey();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    public void updateConfig_updateRootFile() {
        assertThrows("Updating files beyond tenant is forbidden", BusinessException.class, () -> {
            Configuration config = new Configuration("/config/tenants/file.yml", "content");
            configRepository.updateConfigFullPath(config, "hash");
        });

        verifyNoInteractions(tenantContextHolder);
        verifyNoInteractions(kafkaTemplate);
    }

    private ArgumentMatcher<String> isMessageWith(String path, String content, String hash, String tenantKey) {
        return jsonEvent -> {
            try {
                ConfigQueueEvent event = mapper.readValue(jsonEvent, ConfigQueueEvent.class);
                return APP_NAME_TEST.equals(event.getMessageSource())
                    && UPDATE_CONFIG.name().equals(event.getEventType())
                    && tenantKey.equals(event.getTenantKey())
                    && event.getStartDate() != null
                    && event.getData() != null
                    && path.equals(((HashMap) event.getData()).get("path"))
                    && content.equals(((HashMap) event.getData()).get("content"))
                    && hash.equals(((HashMap) event.getData()).get("oldConfigHash"));
            } catch (Exception e) {
                return false;
            }
        };
    }
}

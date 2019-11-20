package com.icthh.xm.commons.config.client.repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TenantConfigRepositoryUnitTest {

    private static final String CONFIG_URL = "http://localhost:8080/config";
    private static final String APP_NAME = "app1";
    private static final String API_TOKEN = "API_TOKEN";

    private TenantConfigRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private XmConfigProperties xmConfigProperties;

    @Mock
    OAuth2Authentication authentication;

    @Before
    public void setUp() {
        when(xmConfigProperties.getXmConfigUrl()).thenReturn(CONFIG_URL);
        repository = new TenantConfigRepository(restTemplate, APP_NAME, xmConfigProperties);

        when(authentication.getDetails()).thenReturn(API_TOKEN);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    @Test
    public void createConfig() {

        repository.createConfig("tenant1", "/path/to/file.txt", "content");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.POST),
                                      refEq(createHttpEntityWithContent()),
                                      eq(Void.class),
                                      eq("TENANT1"));

    }

    @Test
    public void updateConfig() {
        repository.updateConfig("tenant1", "/path/to/file.txt", "content");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.PUT),
                                      refEq(createHttpEntityWithContent()),
                                      eq(Void.class),
                                      eq("TENANT1"));
    }

    @Test
    @SneakyThrows
    public void updateConfigWithOldHash() {

        repository.updateConfig("tenant1", "/path/to/file.txt", "content", "oldHash");

        verify(restTemplate)
            .exchange(eq(CONFIG_URL + "/api/config/tenants/TENANT1/app1/path/to/file.txt?oldConfigHash=oldHash"),
                      eq(HttpMethod.PUT),
                      refEq(createHttpEntityWithContent()),
                      eq(Void.class));

    }

    @Test
    public void deleteConfig() {

        repository.deleteConfig("tenant1", "/path/to/file.txt");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.DELETE),
                                      refEq(createHttpEntityNoContent()),
                                      eq(Void.class),
                                      eq("TENANT1"));

    }

    @Test
    public void createConfigFullPath() {
        repository.createConfigFullPath("tenant1", "/api/config/tenants/{tenantName}/app1/path/to/file.txt", "content");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.POST),
                                      refEq(createHttpEntityWithContent()),
                                      eq(Void.class),
                                      eq("TENANT1"));
    }

    @Test
    public void createConfigsFullPath() {

        List<Configuration> configs = new LinkedList<>();
        configs.add(new Configuration("/config/tenants/{tenantName}/app1/path/to/file1.txt", "content1"));
        configs.add(new Configuration("/config/tenants/TENANT2/app1/path/to/file2.txt", "content2"));

        repository.createConfigsFullPath("tenant1", configs);

        MultiValueMap<String, Object> valueMap = new LinkedMultiValueMap<>();
        valueMap.add("files", new NamedByteArrayResource("content1".getBytes(),
                                                         "/config/tenants/TENANT1/app1/path/to/file1.txt"));
        valueMap.add("files", new NamedByteArrayResource("content2".getBytes(),
                                                         "/config/tenants/TENANT2/app1/path/to/file2.txt"));

        HttpEntity<MultiValueMap> entity = createHttpEntity(valueMap, MediaType.MULTIPART_FORM_DATA);

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config"),
                                      eq(HttpMethod.POST),
                                      refEq(entity),
                                      eq(Void.class));

    }

    @Test
    public void createConfigsFullPathForbiddenPaths() {

        runWithExceptionExpected(
            () -> repository.createConfigsFullPath("tenant-1", singletonList(new Configuration(
                "/config/tenants/tenant2/app1/path/to/file2.txt", "content2")))
            , IllegalArgumentException.class
            , "Tenant name has wrong format: TENANT-1");

        runWithExceptionExpected(
            () -> repository.createConfigsFullPath("tenant1", singletonList(new Configuration(
                "/config/tenants/tenant2/app1/path/to/file2.txt", "content2")))
            , IllegalArgumentException.class
            , "Execution is not allowed for path: /config/tenants/tenant2/app1/path/to/file2.txt");

        runWithExceptionExpected(
            () -> repository.createConfigsFullPath("tenant1", singletonList(new Configuration(
                "/config/tenants/privileges.yml", "content2")))
            , IllegalArgumentException.class
            , "Execution is not allowed for path: /config/tenants/privileges.yml");

    }

    @Test
    public void createConfigs() {

        List<Configuration> configs = new LinkedList<>();
        configs.add(new Configuration("path/to/file1.txt", "content1"));
        configs.add(new Configuration("path/to/file2.txt", "content2"));

        repository.createConfigs("tenant1", configs);

        MultiValueMap<String, Object> expectedMap = new LinkedMultiValueMap<>();
        expectedMap.add("files", new NamedByteArrayResource("content1".getBytes(),
                                                            "/config/tenants/TENANT1/app1/path/to/file1.txt"));
        expectedMap.add("files", new NamedByteArrayResource("content2".getBytes(),
                                                            "/config/tenants/TENANT1/app1/path/to/file2.txt"));

        HttpEntity<MultiValueMap> expected = createHttpEntity(expectedMap, MediaType.MULTIPART_FORM_DATA);

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config"),
                                      eq(HttpMethod.POST),
                                      refEq(expected),
                                      eq(Void.class));

    }

    @Test
    public void updateConfigFullPath() {

        repository.updateConfigFullPath("tenant1", "/api/config/tenants/{tenantName}/app1/path/to/file.txt", "content");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.PUT),
                                      refEq(createHttpEntityWithContent()),
                                      eq(Void.class),
                                      eq("TENANT1"));

    }

    @Test
    @SneakyThrows
    public void updateConfigFullPathWithOldHash() {
        repository.updateConfigFullPath("tenant1",
                                        "/api/config/tenants/{tenantName}/app1/path/to/file.txt",
                                        "content",
                                        "oldHash");

        verify(restTemplate)
            .exchange(eq(CONFIG_URL + "/api/config/tenants/TENANT1/app1/path/to/file.txt?oldConfigHash=oldHash"),
                      eq(HttpMethod.PUT),
                      refEq(createHttpEntityWithContent()),
                      eq(Void.class));
    }

    @Test
    public void deleteConfigFullPath() {

        repository.deleteConfigFullPath("tenant1", "/api/config/tenants/{tenantName}/app1/path/to/file.txt");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.DELETE),
                                      refEq(createHttpEntityNoContent()),
                                      eq(Void.class),
                                      eq("TENANT1"));

    }

    @Test
    public void deleteConfigFullPaths() {

        List<String> fullPaths = new LinkedList<>();
        fullPaths.add("/api/config/tenants/{tenantName}/app1/path/to/file1.txt");
        fullPaths.add("/api/config/tenants/TENANT2/app1/path/to/file2.txt");

        repository.deleteConfigFullPaths("tenant1", fullPaths);

        List<String> fullPathsExpected = new LinkedList<>();
        fullPathsExpected.add("/api/config/tenants/TENANT1/app1/path/to/file1.txt");
        fullPathsExpected.add("/api/config/tenants/TENANT2/app1/path/to/file2.txt");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/"),
                                      eq(HttpMethod.DELETE),
                                      refEq(createHttpEntityWithJsonContent(fullPathsExpected)),
                                      eq(Void.class),
                                      eq("TENANT1"));

    }

    @Test
    public void getConfigFullPath() {

        ResponseEntity<String> response = new ResponseEntity<>("response content", HttpStatus.OK);
        when(restTemplate.exchange(anyString(),
                                   eq(HttpMethod.GET),
                                   any(HttpEntity.class),
                                   eq(String.class),
                                   anyString()))
            .thenReturn(response);

        String result = repository.getConfigFullPath("tenant1",
                                                     "/api/config/tenants/{tenantName}/app1/path/to/file.txt");

        assertEquals("response content", result);

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.GET),
                                      refEq(createHttpEntityNoContent()),
                                      eq(String.class),
                                      eq("TENANT1"));

    }

    @Test
    public void testAssertPathInsideTenant() {

        repository.assertPathInsideTenant("/api/config/tenants/{tenantName}/app1/path/to/file.txt");
        repository.assertPathInsideTenant("/config/tenants/{tenantName}/app1/path/to/file.txt");
        repository.assertPathInsideTenant("/config/tenants/TENANT1/app1/path/to/file.txt");
        repository.assertPathInsideTenant("/config/tenants/TENANT1");
        repository.assertPathInsideTenant("http://localhost:8080/config/api/config/tenants/{tenantName}/app1/file.txt");

        runWithExceptionExpected(() -> repository.assertPathInsideTenant(null),
                                 NullPointerException.class,
                                 "path can not be null");

        runWithExceptionExpected(() -> repository.assertPathInsideTenant("/config/tenants"),
                                 IllegalArgumentException.class,
                                 "Execution is not allowed for path: /config/tenants");

        runWithExceptionExpected(() -> repository.assertPathInsideTenant("/config/tenants/tenant-list.json"),
                                 IllegalArgumentException.class,
                                 "Execution is not allowed for path: /config/tenants/tenant-list.json");

        runWithExceptionExpected(() -> repository.assertPathInsideTenant("/config/tenants/tenant1"),
                                 IllegalArgumentException.class,
                                 "Execution is not allowed for path: /config/tenants/tenant1");

    }

    @Test
    public void assertTenantNameValid() {
        repository.assertTenantNameValid("TENANT1");

        runWithExceptionExpected(() -> repository.assertTenantNameValid(null),
                                 NullPointerException.class,
                                 "tenantName can not be null");

        runWithExceptionExpected(() -> repository.assertTenantNameValid("tenant1"),
                                 IllegalArgumentException.class,
                                 "Tenant name has wrong format: tenant1");

        runWithExceptionExpected(() -> repository.assertTenantNameValid("TENANT-1"),
                                 IllegalArgumentException.class,
                                 "Tenant name has wrong format: TENANT-1");

    }

    private void runWithExceptionExpected(Runnable r, Class<? extends Exception> type, String message) {

        try {
            r.run();
            fail("Expected exception: " + type + " with message: " + message);
        } catch (Exception e) {
            if (!e.getClass().equals(type)) {
                fail("Expected exception.class  : " + type + "\n\t\t\t\tactual was: " + e.getClass());
            }
            if (!String.valueOf(e.getMessage()).equals(message)) {
                fail("Expected exception.message: " + message + "\n\t\t\t\tactual was: " + e.getMessage());
            }
        }

    }

    private HttpEntity<String> createHttpEntityNoContent() {
        return createHttpEntity(null, new MediaType(MediaType.TEXT_PLAIN, UTF_8));
    }

    private HttpEntity<String> createHttpEntityWithContent() {
        return createHttpEntity("content", new MediaType(MediaType.TEXT_PLAIN, UTF_8));
    }

    private <T> HttpEntity<T> createHttpEntityWithJsonContent(T content) {
        return createHttpEntity(content, new MediaType(MediaType.APPLICATION_JSON, UTF_8));
    }

    private <T> HttpEntity<T> createHttpEntity(T content, MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + API_TOKEN);
        return content == null ? new HttpEntity<>(headers) : new HttpEntity<>(content, headers);
    }
}

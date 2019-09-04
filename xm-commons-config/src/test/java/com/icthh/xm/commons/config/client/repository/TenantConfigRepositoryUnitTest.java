package com.icthh.xm.commons.config.client.repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
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

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        when(xmConfigProperties.getXmConfigUrl()).thenReturn(CONFIG_URL);
        repository = new TenantConfigRepository(restTemplate, APP_NAME, xmConfigProperties);

        when(authentication.getDetails()).thenReturn(API_TOKEN);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void createConfig() {

        repository.createConfig("tenant1", "/path/to/file.txt", "content");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.POST),
                                      refEq(createHttpEntityWithContent()),
                                      eq(Void.class),
                                      eq("tenant1"));

    }

    @Test
    public void updateConfig() {
        repository.updateConfig("tenant1", "/path/to/file.txt", "content");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.PUT),
                                      refEq(createHttpEntityWithContent()),
                                      eq(Void.class),
                                      eq("tenant1"));
    }

    @Test
    @SneakyThrows
    public void updateConfigWithOldHash() {

        repository.updateConfig("tenant1", "/path/to/file.txt", "content", "oldHash");

        verify(restTemplate)
            .exchange(eq(CONFIG_URL + "/api/config/tenants/tenant1/app1/path/to/file.txt?oldConfigHash=oldHash"),
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
                                      eq("tenant1"));

    }

    @Test
    public void createConfigFullPath() {
        repository.createConfigFullPath("tenant1", "/api/config/tenants/{tenantName}/app1/path/to/file.txt", "content");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.POST),
                                      refEq(createHttpEntityWithContent()),
                                      eq(Void.class),
                                      eq("tenant1"));
    }

    @Test
    public void createConfigsFullPath() {

        List<Configuration> configs = new LinkedList<>();
        configs.add(new Configuration("/config/tenants/{tenantName}/app1/path/to/file1.txt", "content1"));
        configs.add(new Configuration("/config/tenants/tenant2/app1/path/to/file2.txt", "content2"));

        repository.createConfigsFullPath("tenant1", configs);

        MultiValueMap<String, Object> valueMap = new LinkedMultiValueMap<>();
        valueMap.add("files", new NamedByteArrayResource("content1".getBytes(),
                                                         "/config/tenants/tenant1/app1/path/to/file1.txt"));
        valueMap.add("files", new NamedByteArrayResource("content2".getBytes(),
                                                         "/config/tenants/tenant2/app1/path/to/file2.txt"));

        HttpEntity<MultiValueMap> entity = createHttpEntity(valueMap, MediaType.MULTIPART_FORM_DATA);

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config"),
                                      eq(HttpMethod.POST),
                                      refEq(entity),
                                      eq(Void.class));

    }

    @Test
    public void createConfigs() {

        List<Configuration> configs = new LinkedList<>();
        configs.add(new Configuration("path/to/file1.txt", "content1"));
        configs.add(new Configuration("path/to/file2.txt", "content2"));

        repository.createConfigs("tenant1", configs);

        MultiValueMap<String, Object> valueMap = new LinkedMultiValueMap<>();
        valueMap.add("files", new NamedByteArrayResource("content1".getBytes(),
                                                         "/config/tenants/tenant1/app1/path/to/file1.txt"));
        valueMap.add("files", new NamedByteArrayResource("content2".getBytes(),
                                                         "/config/tenants/tenant1/app1/path/to/file2.txt"));

        HttpEntity<MultiValueMap> entity = createHttpEntity(valueMap, MediaType.MULTIPART_FORM_DATA);

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config"),
                                      eq(HttpMethod.POST),
                                      refEq(entity),
                                      eq(Void.class));

    }

    @Test
    public void updateConfigFullPath() {

        repository.updateConfigFullPath("tenant1", "/api/config/tenants/{tenantName}/app1/path/to/file.txt", "content");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/app1/path/to/file.txt"),
                                      eq(HttpMethod.PUT),
                                      refEq(createHttpEntityWithContent()),
                                      eq(Void.class),
                                      eq("tenant1"));

    }

    @Test
    @SneakyThrows
    public void updateConfigFullPathWithOldHash() {
        repository.updateConfigFullPath("tenant1",
                                        "/api/config/tenants/{tenantName}/app1/path/to/file.txt",
                                        "content",
                                        "oldHash");

        verify(restTemplate)
            .exchange(eq(CONFIG_URL + "/api/config/tenants/tenant1/app1/path/to/file.txt?oldConfigHash=oldHash"),
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
                                      eq("tenant1"));

    }

    @Test
    public void deleteConfigFullPaths() {

        List<String> fullPaths = new LinkedList<>();
        fullPaths.add("/api/config/tenants/{tenantName}/app1/path/to/file1.txt");
        fullPaths.add("/api/config/tenants/tenant2/app1/path/to/file2.txt");

        repository.deleteConfigFullPaths("tenant1", fullPaths);

        List<String> fullPathsExpected = new LinkedList<>();
        fullPathsExpected.add("/api/config/tenants/tenant1/app1/path/to/file1.txt");
        fullPathsExpected.add("/api/config/tenants/tenant2/app1/path/to/file2.txt");

        verify(restTemplate).exchange(eq(CONFIG_URL + "/api/config/tenants/{tenantName}/"),
                                      eq(HttpMethod.DELETE),
                                      refEq(createHttpEntityWithJsonContent(fullPathsExpected)),
                                      eq(Void.class),
                                      eq("tenant1"));

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
                                      eq("tenant1"));

    }

    @Test
    public void testAntPathMatcher() {

//        AntPathMatcher matcher = new AntPathMatcher();
//
////        String pattern = "/**/config/tenants/{tenantName:[A-Z0-9]+}/**";
////        String pattern = "{api}/config/tenants/{tenantName}/**";
//        String pattern = "/**/config/tenants/{tenantName:[A-Z0-9]+|\\{tenantName\\}}/**";
//
//        assertTrue(matcher.match(pattern, "/api/config/tenants/{tenantName}/app1/path/to/file.txt"));
//        assertTrue(matcher.match(pattern, "/config/tenants/{tenantName}/app1/path/to/file.txt"));
//        assertTrue(matcher.match(pattern, "/config/tenants/TENANT1/app1/path/to/file.txt"));
//        assertTrue(matcher.match(pattern, "/config/tenants/TENANT1"));
//
//        assertFalse(matcher.match(pattern, "/config/tenants"));
//        assertFalse(matcher.match(pattern, "/config/tenants/tenant-list.json"));
//        assertFalse(matcher.match(pattern, "/config/tenants/tenant1"));
//
//        System.out.println(matcher.extractUriTemplateVariables(pattern,
//                                                               "/api/config/tenants/TENANT1/app1/path/to/file.txt"));

        repository.assertPathInsideTenant("/api/config/tenants/{tenantName}/app1/path/to/file.txt");
        repository.assertPathInsideTenant("/config/tenants/{tenantName}/app1/path/to/file.txt");
        repository.assertPathInsideTenant("/config/tenants/TENANT1/app1/path/to/file.txt");
        repository.assertPathInsideTenant("/config/tenants/TENANT1");

        expectedEx.expect(IllegalArgumentException.class);

        expectedEx.expectMessage("Execution is not allowed for path: /config/tenants");
        repository.assertPathInsideTenant("/config/tenants");

        expectedEx.expectMessage("Execution is not allowed for path: /config/tenants/tenant-list.json");
        repository.assertPathInsideTenant("/config/tenants/tenant-list.json");

        expectedEx.expectMessage("Execution is not allowed for path: /config/tenants/tenant1");
        repository.assertPathInsideTenant("/config/tenants/tenant1");
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

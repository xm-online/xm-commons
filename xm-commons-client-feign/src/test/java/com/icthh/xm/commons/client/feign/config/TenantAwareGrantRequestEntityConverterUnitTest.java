package com.icthh.xm.commons.client.feign.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.net.URI;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequestEntityConverter;

@RunWith(MockitoJUnitRunner.class)
public class TenantAwareGrantRequestEntityConverterUnitTest {

    @InjectMocks
    private TenantAwareGrantRequestEntityConverter testedInstance;

    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private OAuth2ClientCredentialsGrantRequestEntityConverter defaultConverter;

    @Mock
    private OAuth2ClientCredentialsGrantRequest grantRequest;

    @Test
    public void shouldReturnNullWhenDefaultConverterReturnNull() {
        when(defaultConverter.convert(any())).thenReturn(null);

        RequestEntity<?> requestEntity = testedInstance.convert(grantRequest);

        assertNull(requestEntity);
    }

    @Test
    @SneakyThrows
    public void shouldAddTenantHeaderToRequestEntityWhenDefaultConverterReturnEntity() {
        RequestEntity requestEntity = new RequestEntity("body", HttpMethod.GET, new URI("tst.com"));
        when(defaultConverter.convert(grantRequest)).thenReturn(requestEntity);
        when(tenantContextHolder.getTenantKey()).thenReturn("xm");

        RequestEntity<?> actualEntity = testedInstance.convert(grantRequest);

        assertNotNull(actualEntity);
        HttpHeaders headers = actualEntity.getHeaders();
        assertTrue(headers.containsKey("X-Tenant"));
        List<String> values = headers.get("X-Tenant");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("xm", headers.get("X-Tenant").get(0));
    }
}

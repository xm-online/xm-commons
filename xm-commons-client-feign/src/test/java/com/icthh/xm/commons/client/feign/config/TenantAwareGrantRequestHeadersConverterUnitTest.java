package com.icthh.xm.commons.client.feign.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestHeadersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;

@RunWith(MockitoJUnitRunner.class)
public class TenantAwareGrantRequestHeadersConverterUnitTest {

    @InjectMocks
    private TenantAwareGrantRequestHeadersConverter testedInstance;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private OAuth2ClientCredentialsGrantRequest grantRequest;

    @Mock
    private DefaultOAuth2TokenRequestHeadersConverter<OAuth2ClientCredentialsGrantRequest> delegate;

    @Test
    public void shouldAddTenantHeaderToHeaders() {
        when(delegate.convert(any())).thenReturn(new HttpHeaders());
        when(tenantContextHolder.getTenantKey()).thenReturn("xm");

        HttpHeaders headers = testedInstance.convert(grantRequest);

        assertNotNull(headers);
        assertTrue(headers.containsHeader("X-Tenant"));
        List<String> values = headers.get("X-Tenant");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("xm", values.get(0));
    }

    @Test
    public void shouldAdEnrichTenantHeaderToHeaders() {
        HttpHeaders delegateHeaders = new HttpHeaders();
        delegateHeaders.add("key", "value");

        when(delegate.convert(any())).thenReturn(delegateHeaders);
        when(tenantContextHolder.getTenantKey()).thenReturn("xm");

        HttpHeaders headers = testedInstance.convert(grantRequest);

        assertNotNull(headers);
        assertTrue(headers.containsHeader("X-Tenant"));
        assertTrue(headers.containsHeader("key"));

        List<String> values = headers.get("X-Tenant");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("xm", values.get(0));

        List<String> values1 = headers.get("key");
        assertNotNull(values1);
        assertEquals(1, values1.size());
        assertEquals("value", values1.get(0));
    }
}

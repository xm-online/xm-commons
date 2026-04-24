package com.icthh.xm.commons.client.feign.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestHeadersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;

@RequiredArgsConstructor
public class TenantAwareGrantRequestHeadersConverter
    implements Converter<OAuth2ClientCredentialsGrantRequest, HttpHeaders> {

    private static final String TENANT_HEADER_NAME = "X-Tenant";

    private final TenantContextHolder tenantContextHolder;
    private final DefaultOAuth2TokenRequestHeadersConverter<OAuth2ClientCredentialsGrantRequest> delegate;

    @Override
    public HttpHeaders convert(OAuth2ClientCredentialsGrantRequest source) {
        HttpHeaders headers = delegate.convert(source);
        headers.add(TENANT_HEADER_NAME, tenantContextHolder.getTenantKey());
        return headers;
    }
}

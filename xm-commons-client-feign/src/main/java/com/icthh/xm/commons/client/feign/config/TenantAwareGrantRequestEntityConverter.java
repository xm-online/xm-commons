package com.icthh.xm.commons.client.feign.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.lang.reflect.Type;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequestEntityConverter;

@RequiredArgsConstructor
public class TenantAwareGrantRequestEntityConverter
    implements Converter<OAuth2ClientCredentialsGrantRequest, RequestEntity<?>> {

    private static final String TENANT_HEADER_NAME = "X-Tenant";

    private final TenantContextHolder tenantContextHolder;
    private final OAuth2ClientCredentialsGrantRequestEntityConverter defaultConverter;

    @Override
    public RequestEntity<?> convert(OAuth2ClientCredentialsGrantRequest authorizationGrantRequest) {
        RequestEntity<?> entity = defaultConverter.convert(authorizationGrantRequest);
        if (entity != null) {
            HttpHeaders entityHeaders = HttpHeaders.writableHttpHeaders(entity.getHeaders());
            entityHeaders.add(TENANT_HEADER_NAME, tenantContextHolder.getTenantKey());
            HttpMethod method = entity.getMethod();
            Type type = entity.getType();
            Object body = entity.getBody();
            URI url = entity.getUrl();
            return new RequestEntity<>(body, entityHeaders, method, url, type);
        } else {
            return null;
        }
    }
}

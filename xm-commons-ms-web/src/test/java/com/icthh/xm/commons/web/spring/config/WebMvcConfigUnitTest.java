package com.icthh.xm.commons.web.spring.config;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.commons.web.spring.TenantVerifyInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebMvcConfigUnitTest {

    @Test
    void registersTenantVerifyInterceptor() {
        TenantListRepository tenantListRepository = new TenantListRepository(null,
            new Configuration("/config/tenants/tenants-list.json", "{\"entity\": [{\"name\": \"xm\", \"state\": \"ACTIVE\"}]}"),
            "entity", new XmConfigProperties());
        TenantVerifyInterceptor tenantVerifyInterceptor =
            new TenantVerifyInterceptor(tenantListRepository, new DefaultTenantContextHolder());
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(TenantVerifyInterceptor.class, () -> tenantVerifyInterceptor);
        context.refresh();

        ExposedInterceptorRegistry registry = new ExposedInterceptorRegistry();
        new WebMvcConfig(List.of("/api/token_key"), null, null, context).xmAddInterceptors(registry);

        assertTrue(registry.getInterceptors().stream()
            .map(it -> it instanceof MappedInterceptor mapped ? mapped.getInterceptor() : it)
            .anyMatch(tenantVerifyInterceptor::equals));
    }

    private static class ExposedInterceptorRegistry extends InterceptorRegistry {
        @Override
        public List<Object> getInterceptors() {
            return super.getInterceptors();
        }
    }
}

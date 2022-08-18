package com.icthh.xm.commons.web.spring.config;

import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.TenantVerifyInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.util.List;

@Configuration
@Import({
    XmMsWebConfiguration.class
})
public class WebMvcConfig extends XmWebMvcConfigurerAdapter {

    private final TenantVerifyInterceptor tenantVerifyInterceptor;
    private final List<String> tenantIgnoredPathList;

    public WebMvcConfig(
        @Value("${application.tenant-ignored-path-list}")
        List<String> tenantIgnoredPathList,
        TenantInterceptor tenantInterceptor,
        XmLoggingInterceptor xmLoggingInterceptor,
        TenantVerifyInterceptor tenantVerifyInterceptor) {
        super(tenantInterceptor, xmLoggingInterceptor);
        this.tenantVerifyInterceptor = tenantVerifyInterceptor;
        this.tenantIgnoredPathList = tenantIgnoredPathList;
    }

    @Override
    protected void xmAddInterceptors(InterceptorRegistry registry) {
        registerTenantInterceptorWithIgnorePathPattern(registry, tenantVerifyInterceptor);
    }

    @Override
    protected void xmConfigurePathMatch(PathMatchConfigurer configurer) {
        // no custom configuration
    }

    @Override
    protected List<String> getTenantIgnorePathPatterns() {
        return tenantIgnoredPathList;
    }
}

package com.icthh.xm.commons.web.spring.config;

import com.icthh.xm.commons.tenant.XmRelatedComponent;
import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.util.List;

@Configuration
@Import({
    XmMsWebConfiguration.class
})
public class WebMvcConfig extends XmWebMvcConfigurerAdapter {

    private final List<String> tenantIgnoredPathList;
    private final ApplicationContext applicationContext;

    public WebMvcConfig(
        @Value("${application.tenant-ignored-path-list}")
        List<String> tenantIgnoredPathList,
        TenantInterceptor tenantInterceptor,
        XmLoggingInterceptor xmLoggingInterceptor,
        ApplicationContext applicationContext) {
        super(tenantInterceptor, xmLoggingInterceptor);
        this.applicationContext = applicationContext;
        this.tenantIgnoredPathList = tenantIgnoredPathList;
    }

    @Override
    protected void xmAddInterceptors(InterceptorRegistry registry) {
        applicationContext.getBeansWithAnnotation(XmRelatedComponent.class).values().stream()
            .filter(it -> it instanceof AsyncHandlerInterceptor)
            .map(it -> (AsyncHandlerInterceptor) it)
            .forEach(interceptor -> registerTenantInterceptorWithIgnorePathPattern(registry, interceptor));
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

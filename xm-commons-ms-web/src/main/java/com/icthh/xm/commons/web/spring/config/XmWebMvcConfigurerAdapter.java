package com.icthh.xm.commons.web.spring.config;

import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Objects;

/**
 * The {@link XmWebMvcConfigurerAdapter} class.
 */
public abstract class XmWebMvcConfigurerAdapter implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmWebMvcConfigurerAdapter.class);

    private final TenantInterceptor tenantInterceptor;
    private final XmLoggingInterceptor xmLoggingInterceptor;

    protected XmWebMvcConfigurerAdapter(TenantInterceptor tenantInterceptor,
                                        XmLoggingInterceptor xmLoggingInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
        this.xmLoggingInterceptor = xmLoggingInterceptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addInterceptors(InterceptorRegistry registry) {
        registerTenantInterceptorWithIgnorePathPattern(registry, tenantInterceptor);
        registerXmLoggingInterceptor(registry);

        xmAddInterceptors(registry);
    }

    /**
     * Add Spring MVC lifecycle interceptors for pre- and post-processing of
     * controller method invocations. Interceptors can be registered to apply
     * to all requests or be limited to a subset of URL patterns.
     *
     * @see WebMvcConfigurer#addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry)
     */
    protected abstract void xmAddInterceptors(InterceptorRegistry registry);

    /**
     * {@inheritDoc}
     */
    @Override
    public final void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false);

        xmConfigurePathMatch(configurer);
    }

    /**
     * Helps with configuring HandlerMappings path matching options such as trailing slash match,
     * suffix registration, path matcher and path helper.
     * Configured path matcher and path helper instances are shared for:
     * <ul>
     * <li>RequestMappings</li>
     * <li>ViewControllerMappings</li>
     * <li>ResourcesMappings</li>
     * </ul>
     *
     * @see WebMvcConfigurer#configurePathMatch(org.springframework.web.servlet.config.annotation.PathMatchConfigurer)
     */
    protected abstract void xmConfigurePathMatch(PathMatchConfigurer configurer);

    /**
     * Registered interceptor to all request except passed urls.
     * @param registry helps with configuring a list of mapped interceptors.
     * @param interceptor the interceptor
     */
    protected void registerTenantInterceptorWithIgnorePathPattern(
                    InterceptorRegistry registry, HandlerInterceptor interceptor) {
        InterceptorRegistration tenantInterceptorRegistration = registry.addInterceptor(interceptor);
        tenantInterceptorRegistration.addPathPatterns("/**");

        List<String> tenantIgnorePathPatterns = getTenantIgnorePathPatterns();
        Objects.requireNonNull(tenantIgnorePathPatterns, "tenantIgnorePathPatterns can't be null");

        for (String pattern : tenantIgnorePathPatterns) {
            tenantInterceptorRegistration.excludePathPatterns(pattern);
        }

        LOGGER.info("Added handler interceptor '{}' to all urls, exclude {}", interceptor.getClass()
                        .getSimpleName(), tenantIgnorePathPatterns);
    }

    /**
     * Return  tenant context ignore URL patterns.
     *
     * @return non {@code null} list of tenant context ignore URL patterns, can be empty
     */
    protected abstract List<String> getTenantIgnorePathPatterns();

    private void registerXmLoggingInterceptor(InterceptorRegistry registry) {
        registry.addInterceptor(xmLoggingInterceptor).addPathPatterns("/**");

        LOGGER.info("Added handler interceptor '{}' to all urls", XmLoggingInterceptor.class.getSimpleName());
    }

}

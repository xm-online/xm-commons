package com.icthh.xm.commons.domainevent.service;

import com.icthh.xm.commons.domainevent.config.FilterConfig;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.service.filter.WebLepFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
public class WebFilterEngine {

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final List<String> ignoredPatterns;
    private final List<String> ignoredHttpMethods;

    private final XmDomainEventConfiguration xmDomainEventConfiguration;
    private final WebLepFilter webLepFilter;

    private final boolean isApplicationLevelFilter;

    public WebFilterEngine(@Value("${application.tenant-ignored-path-list:#{T(java.util.Collections).emptyList()}}") List<String> ignoredPatterns,
                           @Value("${application.timeline-ignored-http-methods:#{T(java.util.Collections).emptyList()}}") List<String> ignoredHttpMethods,
                           XmDomainEventConfiguration xmDomainEventConfiguration, WebLepFilter webLepFilter) {
        this.ignoredPatterns = ignoredPatterns;
        this.ignoredHttpMethods = ignoredHttpMethods;
        this.xmDomainEventConfiguration = xmDomainEventConfiguration;
        this.webLepFilter = webLepFilter;
        this.isApplicationLevelFilter = isApplicationFilterEnabled(ignoredPatterns, ignoredHttpMethods);
    }

    public DomainEvent isIgnoreRequest(HttpServletRequest request, HttpServletResponse response, String tenant,
                                       Supplier<DomainEvent> domainEventSupplier) {

        // find filter config for tenant and filtering by http method, response status and math url request url
        FilterConfig filterConfig = xmDomainEventConfiguration.getFilterListByTenant(tenant).stream()
            .filter(c -> c.getHttpOperation().contains(request.getMethod()))
            .filter(c -> c.getResponseCode().contains(response.getStatus()))
            .filter(c -> matcher.match(c.getUrlPattern(), request.getRequestURI()))
            .findAny()
            .orElse(null);

        // no filter config, but exist application level filter
        if (filterConfig == null && isApplicationLevelFilter) {
            if (isApplicationLevelFilterRequest(request)){
                return null;
            }
        }

        // no filter config and application level is false
        if (filterConfig == null) {
            return null;
        }

        DomainEvent domainEvent = domainEventSupplier.get();
        boolean isLepFiltering = webLepFilter.lepFiltering(filterConfig.getKey(), domainEvent);
        if (!isLepFiltering) {
            return null;
        }

        return domainEvent;
    }

    private boolean isApplicationFilterEnabled(List<String> ignoredPatterns, List<String> ignoredHttpMethods) {
        return CollectionUtils.isEmpty(ignoredPatterns) || CollectionUtils.isEmpty(ignoredHttpMethods);
    }

    private boolean isApplicationLevelFilterRequest(HttpServletRequest request) {

        String path = request.getServletPath();
        String httpMethod = request.getMethod();

        if (CollectionUtils.isNotEmpty(ignoredHttpMethods) && ignoredHttpMethods.contains(httpMethod)) {
            return true;
        }

        if (ignoredPatterns != null && path != null) {
            for (String pattern : ignoredPatterns) {
                if (matcher.match(pattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }
}

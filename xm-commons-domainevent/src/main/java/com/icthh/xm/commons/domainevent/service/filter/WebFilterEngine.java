package com.icthh.xm.commons.domainevent.service.filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.icthh.xm.commons.domainevent.config.FilterConfig;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.service.filter.lep.WebLepFilter;
import com.icthh.xm.commons.domainevent.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@Slf4j
@Service
public class WebFilterEngine {

    private static final String EXCLUDE = "exclude";
    private static final String INCLUDE = "include";

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final List<String> ignoredPatterns;
    private final List<String> ignoredHttpMethods;

    private final XmDomainEventConfiguration xmDomainEventConfiguration;
    private final WebLepFilter webLepFilter;
    private final JsonFactory jFactory;

    private final boolean isApplicationLevelFilter;

    public WebFilterEngine(@Value("${application.tenant-ignored-path-list:#{T(java.util.Collections).emptyList()}}") List<String> ignoredPatterns,
                           @Value("${application.timeline-ignored-http-methods:#{T(java.util.Collections).emptyList()}}") List<String> ignoredHttpMethods,
                           XmDomainEventConfiguration xmDomainEventConfiguration, WebLepFilter webLepFilter) {
        this.ignoredPatterns = ignoredPatterns;
        this.ignoredHttpMethods = ignoredHttpMethods;
        this.xmDomainEventConfiguration = xmDomainEventConfiguration;
        this.webLepFilter = webLepFilter;
        this.isApplicationLevelFilter = isApplicationFilterEnabled(ignoredPatterns, ignoredHttpMethods);
        this.jFactory = new JsonFactory();
    }

    public DomainEvent isIgnoreRequest(HttpServletRequest request, HttpServletResponse response, String tenant,
                                       BiFunction<String[], String, DomainEvent> domainEventFactory) {

        // find filter config for tenant and filtering by http method, response status and math url request url
        List<FilterConfig> filterListByTenant = xmDomainEventConfiguration.getFilterListByTenant(tenant);

        // no filter config, but exist application level filter
        if (isApplicationLevelFilter && filterListByTenant.isEmpty()) {
            if (isApplicationLevelFilterRequest(request)) {
                return null;
            }
        }

        String responseBody = WebApiDomainEventFactory.getResponseContent(response);
        String[] values = JsonUtil.extractIdAndTypeKey(jFactory, responseBody);
        String aggregationType = JsonUtil.AggregateMapper.getTypeKey(values);

        FilterConfig filterConfig = filterListByTenant.stream()
            .filter(getHttpOperationPredicate(request.getMethod()))
            .filter(getResponseCodePredicate(response.getStatus()))
            .filter(getAggregationTypePredicate(aggregationType))
            .filter(getRequestUrlPredicate(request.getRequestURI()))
            .findAny()
            .orElse(null);

        // no filter config and application level is false
        if (filterConfig == null || EXCLUDE.equals(filterConfig.getFilterType())) {
            return null;
        }

        DomainEvent domainEvent = domainEventFactory.apply(values, responseBody);
        boolean isLepFiltering = webLepFilter.lepFiltering(filterConfig.getKey(), domainEvent);
        if (!isLepFiltering) {
            return null;
        }

        return domainEvent;
    }

    private Predicate<FilterConfig> getAggregationTypePredicate(String aggregationType) {
        return (config) -> {
            if (config.getAggregateType() == null) {
                return true;
            }

            if (config.getAggregateType().isEmpty()) {
                return false;
            }

            return config.getAggregateType().contains(aggregationType);
        };
    }

    private Predicate<FilterConfig> getRequestUrlPredicate(String requestUrl) {
        return (config) -> {

            if (StringUtils.isEmpty(config.getUrlPattern())) {
                return true;
            }

            if ("*".equals(config.getUrlPattern())) {
                return true;
            }

            return matcher.match(config.getUrlPattern(), requestUrl);
        };
    }

    private Predicate<FilterConfig> getHttpOperationPredicate(String httpOperation) {
        return (config) -> {
            if (config.getHttpOperation() == null) {
                return true;
            }

            if (config.getHttpOperation().isEmpty()) {
                return false;
            }

            return config.getHttpOperation().contains(httpOperation);
        };
    }

    private Predicate<FilterConfig> getResponseCodePredicate(int responseCode) {
        return (config) -> {
            if (config.getResponseCode() == null) {
                return true;
            }

            if (config.getResponseCode().isEmpty()) {
                return false;
            }

            return config.getResponseCode().contains(responseCode);
        };
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

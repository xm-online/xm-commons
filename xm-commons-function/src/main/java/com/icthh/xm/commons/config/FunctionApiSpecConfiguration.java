package com.icthh.xm.commons.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domain.spec.FunctionApiSpec;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@IgnoreLogginAspect
public class FunctionApiSpecConfiguration implements RefreshableConfiguration { // todo: do we need MapRefreshableConfiguration (???)

    private static final String TENANT_NAME = "tenantName";
    private static final String DEFAULT_PATH_PATTERN = "/config/tenants/{tenantName}/functions.yml";

    private final String pathPattern;
    private final AntPathMatcher matcher;
    private final ObjectMapper objectMapper;
    private final Map<String, FunctionApiSpec> functionSpecsByTenant;

    public FunctionApiSpecConfiguration(@Value("application.function-spec-path-pattern") String pathPattern) {
        this.pathPattern = getPathPatternOrDefault(pathPattern);
        this.matcher = new AntPathMatcher();
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.functionSpecsByTenant = new ConcurrentHashMap<>();
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = getTenantName(updatedKey);
            if (StringUtils.isBlank(config)) {
                functionSpecsByTenant.remove(tenant);
                log.info("Function spec for tenant {} was removed due to empty", tenant);
            } else {
                functionSpecsByTenant.put(tenant, objectMapper.readValue(config, FunctionApiSpec.class));
                log.info("Function spec for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error when read function spec from path {}: ", updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(pathPattern, updatedKey);
    }

    public Optional<FunctionApiSpec> getSpecByTenant(String tenantKey) {
        return Optional.ofNullable(functionSpecsByTenant.get(tenantKey));
    }

    private String getTenantName(String updatedKey) {
        return matcher.extractUriTemplateVariables(pathPattern, updatedKey).get(TENANT_NAME);
    }

    private String getPathPatternOrDefault(String pathPattern) {
        return StringUtils.isNoneEmpty(pathPattern) ? pathPattern : DEFAULT_PATH_PATTERN;
    }
}

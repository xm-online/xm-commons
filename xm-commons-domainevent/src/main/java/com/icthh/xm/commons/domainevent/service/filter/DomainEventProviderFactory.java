package com.icthh.xm.commons.domainevent.service.filter;

import com.icthh.xm.commons.domainevent.config.ApiMaskConfig;
import com.icthh.xm.commons.domainevent.config.ApiMaskRule;
import com.icthh.xm.commons.domainevent.config.XmDomainEventConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "application.domain-event.enabled", havingValue = "true")
public class DomainEventProviderFactory {

    @Value("${spring.application.name}")
    private String appName;
    private final XmDomainEventConfiguration xmDomainEventConfiguration;
    private final ApiMaskConfig apiIgnore;

    public DomainEventProvider newDomainEventProvider() {
        List<ApiMaskRule> maskRules = apiIgnore != null ? apiIgnore.getMaskRules() : null;
        return new DomainEventProviderIml(appName, xmDomainEventConfiguration, maskRules);
    }
}

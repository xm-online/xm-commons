package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.lep.RefreshTaskExecutor;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(LepSpringConfiguration.class)
public class LepSpringConfiguration {

    private final String appName;

    public LepSpringConfiguration(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public XmLepScriptConfigServerResourceLoader cfgResourceLoader(LepManagementService lepManagementService,
                                                                   RefreshTaskExecutor refreshTaskExecutor) {
        return new XmLepScriptConfigServerResourceLoader(appName, lepManagementService, refreshTaskExecutor);
    }

    @Bean
    @ConditionalOnMissingBean(RefreshTaskExecutor.class)
    public RefreshTaskExecutor refreshTaskExecutor() {
        return new RefreshTaskExecutor();
    }

}

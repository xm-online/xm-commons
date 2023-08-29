package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.impl.utils.ClassPathLepRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroovyLepEngineConfiguration {

    @Bean
    public GroovyLepEngineFactory groovyLepEngineFactory(@Value("${spring.application.name}") String appName,
                                                         ClassPathLepRepository classPathLepRepository,
                                                         TenantAliasService tenantAliasService) {
        return new GroovyLepEngineFactory(appName, classPathLepRepository, tenantAliasService);
    }

}

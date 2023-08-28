package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.RefreshTaskExecutor;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 */
@Configuration
@ComponentScan({"com.icthh.xm.commons.lep", "com.icthh.xm.lep"})
@EnableAutoConfiguration
@Profile("resolveclasstest")
public class DynamicLepTestConfig extends LepSpringConfiguration {

    public DynamicLepTestConfig() {
        super("testApp");
    }

    @Bean
    public DynamicTestLepService testLepService() {
        return new DynamicTestLepService();
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasService();
    }

    @Bean
    public RefreshTaskExecutor refreshTaskExecutor() {
        return new RefreshTaskExecutor() {
            CallerRunsPolicy callerRunsPolicy = new ThreadPoolExecutor.CallerRunsPolicy();
            @Override
            public void execute(Runnable command) {
                callerRunsPolicy.rejectedExecution(command, this);
            }
        };
    }

    @Bean
    public XmLepScriptConfigServerResourceLoader cfgResourceLoader(LepManagementService lepManagementService,
                                                                   RefreshTaskExecutor refreshTaskExecutor) {
        var resourceLoader = new XmLepScriptConfigServerResourceLoader("testApp", lepManagementService, refreshTaskExecutor) {
            @Override
            public void onRefresh(String updatedKey, String configContent) {
                super.onRefresh(updatedKey, configContent);
                refreshFinished(List.of(updatedKey));
            }
        };
        resourceLoader.refreshFinished(List.of());
        return resourceLoader;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();

        String properties = "spring.application.name=testApp";
        Resource propertiesResource = new ByteArrayResource(properties.getBytes(UTF_8));

        pspc.setLocation(propertiesResource);
        pspc.setIgnoreResourceNotFound(false);
        pspc.setIgnoreUnresolvablePlaceholders(false);

        return pspc;
    }

}

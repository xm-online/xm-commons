package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.groovy.TenantScriptStorageTypeProvider;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import lombok.SneakyThrows;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PreDestroy;

@Configuration
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
@Profile("resolvefiletest")
public class DynamicLepTestFileConfig extends DynamicLepTestConfig {

    private final TemporaryFolder folder = new TemporaryFolder();

    @SneakyThrows
    public DynamicLepTestFileConfig() {
        folder.create();
    }

    @Bean
    public TenantScriptStorageTypeProvider getTenantScriptStorageType() {
        return () -> TenantScriptStorage.FILE;
    }

    @Override
    protected String getFileTenantScriptPathResolverBaseDir() {
        return folder.getRoot().toPath().toFile().getAbsolutePath();
    }

    @Bean
    public TemporaryFolder folder() {
        return folder;
    }

    @PreDestroy
    public void preDestroy() {
        folder.delete();
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

}

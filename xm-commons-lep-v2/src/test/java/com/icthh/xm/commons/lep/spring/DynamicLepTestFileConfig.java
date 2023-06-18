package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import lombok.SneakyThrows;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PreDestroy;

@Configuration
@EnableLepServices(basePackageClasses = DynamicTestLepService.class)
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
@Profile("resolvefiletest")
public class DynamicLepTestFileConfig extends DynamicLepTestConfig {

    private final TemporaryFolder folder = new TemporaryFolder();

    @SneakyThrows
    public DynamicLepTestFileConfig(final ApplicationEventPublisher eventPublisher,
                                    final ResourceLoader resourceLoader) {
        super(eventPublisher, resourceLoader);
        folder.create();
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.FILE;
    }


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

}

package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ComponentScan("com.icthh.xm.commons.lep.spring")
@EnableAutoConfiguration
@Profile("resolvedirtytest")
public class DynamicDirtyClassPathLepTestConfig extends DynamicLepTestConfig {

    @Override
    public TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.XM_MS_CONFIG_DIRTY_CLASSPATH;
    }

}

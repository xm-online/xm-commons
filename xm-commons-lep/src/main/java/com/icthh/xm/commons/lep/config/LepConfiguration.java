package com.icthh.xm.commons.lep.config;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.spring.EnableLepServices;
import com.icthh.xm.commons.lep.spring.web.WebLepSpringConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableLepServices
// For customization configuration you need to extend WebLepSpringConfiguration
public class LepConfiguration extends WebLepSpringConfiguration {

    @Value("${application.lep.tenant-script-storage:#{T(com.icthh.xm.commons.lep.TenantScriptStorage).XM_MS_CONFIG}}")
    private TenantScriptStorage tenantScriptStorageType;

    public LepConfiguration(@Value("${spring.application.name}") String appName,
                            ApplicationEventPublisher eventPublisher,
                            ResourceLoader resourceLoader) {
        super(appName, eventPublisher, resourceLoader);
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return tenantScriptStorageType;
    }

}

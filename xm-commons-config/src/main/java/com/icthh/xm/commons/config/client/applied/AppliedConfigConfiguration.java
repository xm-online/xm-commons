package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the applied config version API. Disable with {@code application.applied-config.enabled=false}.
 */
@Configuration
@ConditionalOnProperty("xm-config.enabled")
@ConditionalOnProperty(name = "application.applied-config.enabled", havingValue = "true", matchIfMissing = true)
public class AppliedConfigConfiguration {

    private static final String FILE_MODE = "FILE";

    @Bean
    public AppliedConfigService appliedConfigService(ConfigService configService,
                                                     CommonConfigRepository repository,
                                                     FetchConfigurationSettings fetchSettings,
                                                     XmConfigProperties xmConfigProperties) {
        boolean versioned = !FILE_MODE.equalsIgnoreCase(xmConfigProperties.getConfigMode());
        AppliedConfigService service = new AppliedConfigService(repository, fetchSettings, versioned);
        configService.addConfigurationChangedListener(service);
        return service;
    }
}

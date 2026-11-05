package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.security.spring.config.SecurityConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

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

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class AppliedConfigWebConfiguration {

        @Bean
        public AppliedConfigResource appliedConfigResource(
            AppliedConfigService service,
            ObjectProvider<XmAuthenticationContextHolder> authContextHolder,
            ObjectProvider<XmPermissionCheckService> permissionCheckService,
            @Value("${application.management.access.role:SYSTEM-ADMIN}") String managementRole) {
            return new AppliedConfigResource(service, authContextHolder, permissionCheckService, managementRole);
        }

        @Bean
        public RouterFunction<ServerResponse> appliedConfigRoute(AppliedConfigResource resource) {
            return RouterFunctions.route().GET(SecurityConfiguration.APPLIED_CONFIG_PATH, resource::get).build();
        }
    }
}

package com.icthh.xm.commons.config.defaults;

import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties;
import org.springframework.boot.actuate.health.HealthStatusHttpMapper;
import org.springframework.boot.actuate.health.HealthWebEndpointResponseMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = {
    "org.springframework.boot.actuate.health.HealthWebEndpointResponseMapper",
    "org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties"
})
public class XmHealthDetailsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HealthWebEndpointResponseMapper healthWebEndpointResponseMapper(HealthStatusHttpMapper statusHttpMapper,
                                                                           HealthEndpointProperties properties) {
        return new AuthorityHealthWebEndpointResponseMapper(statusHttpMapper, properties.getShowDetails(),
            properties.getRoles());
    }
}

package com.icthh.xm.commons.permission.service.custom;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CustomPrivilegeSpecConfiguration {

    @Bean
    @ConditionalOnMissingBean(CustomPrivilegeSpecService.class)
    public CustomPrivilegeSpecService customPrivilegeSpecService(CommonConfigRepository commonConfigRepository,
                                                            List<CustomPrivilegesExtractor> privilegesExtractors) {
        return new AbstractCustomPrivilegeSpecService(commonConfigRepository, privilegesExtractors) {
        };
    }
}

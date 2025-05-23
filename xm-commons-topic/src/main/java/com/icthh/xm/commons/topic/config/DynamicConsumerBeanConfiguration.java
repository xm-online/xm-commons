package com.icthh.xm.commons.topic.config;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfiguration;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfigurationService;
import com.icthh.xm.commons.topic.service.TopicManagerService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;


@Configuration
@ConditionalOnExpression("${xm-config.enabled} && ${tenant.reject-suspended:true}")
public class DynamicConsumerBeanConfiguration {

    @Bean
    public DynamicConsumerConfigurationService dynamicConsumerConfigurationService(@Lazy List<DynamicConsumerConfiguration> dynamicConsumerConfigurations,
                                                                                   TopicManagerService topicManagerService,
                                                                                   TenantListRepository tenantListRepository) {
        return new DynamicConsumerConfigurationService(
            dynamicConsumerConfigurations,
            topicManagerService,
            tenantListRepository
        );
    }

}

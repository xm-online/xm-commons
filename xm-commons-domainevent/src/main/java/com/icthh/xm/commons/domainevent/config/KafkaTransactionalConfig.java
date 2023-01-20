package com.icthh.xm.commons.domainevent.config;

import com.icthh.xm.commons.domainevent.service.imp.KafkaTransactionSynchronizationAdapter;
import com.icthh.xm.commons.domainevent.service.imp.KafkaTransactionSynchronizationAdapterService;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.SimpleTransactionScope;

@Configuration
public class KafkaTransactionalConfig {

    public KafkaTransactionalConfig(ConfigurableListableBeanFactory factory) {
        factory.registerScope("transaction", new SimpleTransactionScope());
    }

    @Bean
    public KafkaTransactionSynchronizationAdapterService kafkaTransactionSynchronizationAdapterService(ApplicationContext context) {
        return new KafkaTransactionSynchronizationAdapterService() {
            @Override
            public KafkaTransactionSynchronizationAdapter getKafkaTransactionSynchronizationAdapter() {
                return context.getBean(KafkaTransactionSynchronizationAdapter.class);
            }
        };
    }
}

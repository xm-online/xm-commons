package com.icthh.xm.commons.domainevent.outbox.config;

import com.icthh.xm.commons.domainevent.config.event.InitSourceEvent;
import com.icthh.xm.commons.domainevent.outbox.service.impl.OutboxTransport;
import com.icthh.xm.commons.migration.db.liquibase.LiquibaseRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class OutboxInitSourceEventListener implements ApplicationListener<InitSourceEvent> {
    private static final String OUTBOX_CHANGE_LOG_PATH
        = "classpath:config/liquibase/changelog/outbox/db.changelog-master.yaml";
    private final LiquibaseRunner liquibaseRunner;
    private final String outboxTransportBeanName;

    public OutboxInitSourceEventListener(ApplicationContext applicationContext,
                                         LiquibaseRunner liquibaseRunner) {
        this.liquibaseRunner = liquibaseRunner;
        this.outboxTransportBeanName = applicationContext.getBeanNamesForType(OutboxTransport.class)[0];
    }

    @Override
    public void onApplicationEvent(InitSourceEvent event) {
        if (outboxTransportBeanName.equals(event.getTransport())) {
            liquibaseRunner.runOnTenant(event.getTenantKey(), OUTBOX_CHANGE_LOG_PATH);
        }
    }
}

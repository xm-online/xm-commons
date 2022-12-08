package com.icthh.xm.commons.domainevent.db.service.builder;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.service.builder.DomainEventBuilder;
import com.icthh.xm.commons.domainevent.service.builder.impl.DefaultDomainEventBuilder;
import com.icthh.xm.commons.migration.db.DatabaseTxIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("txDomainEventBuilder")
@RequiredArgsConstructor
public class TransactionalDomainEventBuilder implements DomainEventBuilder {

    private final DefaultDomainEventBuilder defaultDomainEventBuilder;
    private final DatabaseTxIdResolver databaseTxIdResolver;

    @Override
    public DomainEvent.DomainEventBuilder getPrefilledBuilder() {
        return defaultDomainEventBuilder.getPrefilledBuilder()
            .txId(databaseTxIdResolver.getDatabaseTransactionId());
    }
}

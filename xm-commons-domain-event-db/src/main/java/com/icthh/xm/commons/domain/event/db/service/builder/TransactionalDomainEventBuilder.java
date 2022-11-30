package com.icthh.xm.commons.domain.event.db.service.builder;

import com.icthh.xm.commons.domain.event.domain.DomainEvent;
import com.icthh.xm.commons.domain.event.service.builder.DomainEventBuilder;
import com.icthh.xm.commons.domain.event.service.builder.impl.DefaultDomainEventBuilder;
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

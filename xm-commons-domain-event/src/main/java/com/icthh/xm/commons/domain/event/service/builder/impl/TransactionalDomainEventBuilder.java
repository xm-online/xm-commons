package com.icthh.xm.commons.domain.event.service.builder.impl;

import com.icthh.xm.commons.domain.event.service.TransactionService;
import com.icthh.xm.commons.domain.event.service.builder.DomainEventBuilder;
import com.icthh.xm.commons.domain.event.service.dto.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("txDomainEventBuilder")
@RequiredArgsConstructor
class TransactionalDomainEventBuilder implements DomainEventBuilder {

    private final DefaultDomainEventBuilder defaultDomainEventBuilder;
    private final TransactionService transactionService;

    @Override
    public DomainEvent.DomainEventBuilder getPrefilledBuilder() {
        return defaultDomainEventBuilder.getPrefilledBuilder()
            .txId(transactionService.getDatabaseTransactionId());
    }
}

package com.icthh.xm.commons.domainevent.service.impl;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@Scope("transaction")
public class KafkaTransactionSynchronizationAdapter {

    private final List<DomainEvent> eventList = new ArrayList<>();

    public void send(DomainEvent event, Consumer<DomainEvent> kafkaTransport) {
        if (eventList.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCompletion(int status) {
                    super.afterCompletion(status);
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        try {
                            if (!eventList.isEmpty()) {
                                eventList.forEach(kafkaTransport);
                                log.debug("Inserted {} items to kafka", eventList.size());
                            }
                        } catch (Exception e) {
                            log.error("Error in afterCompletion stage during transaction synchronization for entity: {}",
                                eventList, e);
                        }
                    }
                    eventList.clear();
                }
            });
        }
        eventList.add(event);
    }
}

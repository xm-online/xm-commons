package com.icthh.xm.commons.domainevent.service.imp;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.service.Transport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaTransport implements Transport {

    @Override
    public void send(DomainEvent event) {
        log.info("Send event to kafka");
    }
}

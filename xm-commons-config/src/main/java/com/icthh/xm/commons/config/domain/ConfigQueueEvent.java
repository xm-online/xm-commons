package com.icthh.xm.commons.config.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigQueueEvent {

    private String eventId;
    private String messageSource;
    private String tenantKey;
    private String eventType;
    private Instant startDate = Instant.now();
    private Object data;

}

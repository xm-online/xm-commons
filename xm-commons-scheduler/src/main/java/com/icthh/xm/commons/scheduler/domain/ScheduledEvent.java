package com.icthh.xm.commons.scheduler.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Scheduled event model to be send to stream.
 */
@Getter
@Setter
@ToString
public class ScheduledEvent {

    private String uuid;

    private Long id;

    private String key;

    private String name;

    private String typeKey;

    private String stateKey;

    private String createdBy;

    private Instant startDate;

    private Instant endDate;

    private Instant handlingTime;

    private ChannelType channelType;

    private Map<String, Object> data;

}

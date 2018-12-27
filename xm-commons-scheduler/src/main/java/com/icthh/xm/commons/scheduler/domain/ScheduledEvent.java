package com.icthh.xm.commons.scheduler.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Scheduled event model to be send to stream.
 */
@Getter
@Setter
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

    @Override
    public String toString() {
        return "ScheduledEvent{"
            + "id=" + id
            + ", key='" + key + '\''
            + ", uuid='" + uuid + '\''
            + ", name='" + name + '\''
            + ", typeKey='" + typeKey + '\''
            + ", stateKey='" + stateKey + '\''
            + ", createdBy='" + createdBy + '\''
            + ", startDate=" + startDate
            + ", handlingTime=" + handlingTime
            + ", endDate=" + endDate
            + ", channelType=" + channelType
            + ", data.size=" + Optional.ofNullable(data).map(Map::size).orElse(0)
            + '}';
    }
}

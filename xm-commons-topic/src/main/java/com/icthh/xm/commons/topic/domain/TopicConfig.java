package com.icthh.xm.commons.topic.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class TopicConfig implements Serializable {

    private String key;
    private String typeKey;
    private String topicName;
    private Integer retriesCount;
    private Long backOffPeriod;
    private String deadLetterQueue;
    private String groupId;
    private Boolean logBody = true;
    private Integer maxPollInterval;
    private String isolationLevel;
}

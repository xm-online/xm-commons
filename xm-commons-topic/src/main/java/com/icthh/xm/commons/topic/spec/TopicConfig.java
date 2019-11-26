package com.icthh.xm.commons.topic.spec;

import lombok.Data;

import java.io.Serializable;

@Data
public class TopicConfig implements Serializable {

    private String topicName;
    private Integer retriesCount;
    private Long backOffPeriod;
    private String handlerName;
    private String deadLetterQueue;
    private String groupId;
}

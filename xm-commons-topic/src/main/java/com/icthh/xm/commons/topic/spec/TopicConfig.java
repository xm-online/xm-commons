package com.icthh.xm.commons.topic.spec;

import lombok.Data;

import java.io.Serializable;

@Data
public class TopicConfig implements Serializable {

    private String topicName;
    private int retriesCount;
    private String handlerName;
    private String deadLetterQueue;
    private String groupId;
}

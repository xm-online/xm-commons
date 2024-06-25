package com.icthh.xm.commons.topic.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "xm-topic.auto-create")
public class AutoCreateTopicConfiguration {

    private boolean enabled;
    private List<AutoCreateTopicConfig> config;

    @Getter
    @Setter
    public static class AutoCreateTopicConfig {
        private String topicName;
        private Integer replicationFactor;
        private Integer numPartitions;
    }

}

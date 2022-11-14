package com.icthh.xm.commons.domain.event.config;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class EventPublisherConfig {
    private boolean enabled;
    private Map<String, SourceConfig> sources = new HashMap<>();
    private PublisherConfig publisher;
}

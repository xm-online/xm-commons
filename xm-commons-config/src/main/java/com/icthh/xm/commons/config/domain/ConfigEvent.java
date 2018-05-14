package com.icthh.xm.commons.config.domain;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ConfigEvent {

    private String eventId;
    private List<ConfigurationEvent> configurations = Collections.emptyList();
}

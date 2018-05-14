package com.icthh.xm.commons.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Data
public class ConfigEvent {

    private String eventId;
    private String messageSource;

    @JsonIgnore
    private Instant startDate = Instant.now();
    private List<ConfigurationEvent> configurations = Collections.emptyList();

    @JsonProperty("startDate")
    public String getStartDate() {
        return startDate.toString();
    }
}

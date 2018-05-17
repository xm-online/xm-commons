package com.icthh.xm.commons.config.domain;

import lombok.Data;

import java.util.Collections;
import java.util.Set;

@Data
public class ConfigEvent {

    private String eventId;
    private String commit;
    private Set<String> paths = Collections.emptySet();
}

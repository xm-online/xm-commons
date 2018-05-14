package com.icthh.xm.commons.config.domain;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ConfigEvent {

    private String eventId;
    private String commit;
    private List<String> paths = Collections.emptyList();
}

package com.icthh.xm.commons.domainevent.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceConfig {

    private boolean enabled;
    private String transport;
    private Filter filter;
    private TransformMappingConfig transform;
}

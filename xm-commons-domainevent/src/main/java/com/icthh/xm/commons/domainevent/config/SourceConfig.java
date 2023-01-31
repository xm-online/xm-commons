package com.icthh.xm.commons.domainevent.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter
@Setter
@Slf4j
public class SourceConfig {
    private boolean enabled;
    private String transport;
    private TransformMappingConfig transform;
    private List<FilterConfig> filter;
}

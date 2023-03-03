package com.icthh.xm.commons.domainevent.config;

import lombok.Data;

import java.util.List;

@Data
public class TransformMappingConfig {

    private String operationName;
    private String urlPattern;
    private List<String> httpOperation;
}

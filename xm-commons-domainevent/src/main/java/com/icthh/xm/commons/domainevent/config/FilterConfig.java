package com.icthh.xm.commons.domainevent.config;

import lombok.Data;

import java.util.List;

@Data
public class FilterConfig {
    private String key;
    private List<Integer> responseCode;
    private String urlPattern;
    private List<String> httpOperation;
    private List<String> aggregateType;
}

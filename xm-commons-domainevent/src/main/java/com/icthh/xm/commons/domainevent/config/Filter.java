package com.icthh.xm.commons.domainevent.config;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Filter {

    private String key;
    private Map<String, List<EntityFilter>> dsl = new HashMap<>();
}

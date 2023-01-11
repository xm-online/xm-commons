package com.icthh.xm.commons.domainevent.config;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Query {

    private Map<String, Column> columns = new HashMap<>();
}

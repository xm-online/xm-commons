package com.icthh.xm.commons.domainevent.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class Query {

    private Map<String, Column> columns = new HashMap<>();
}

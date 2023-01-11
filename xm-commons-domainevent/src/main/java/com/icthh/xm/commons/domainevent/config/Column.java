package com.icthh.xm.commons.domainevent.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Column {

    private List<String> in;
    private List<String> notIn;
    private String equals;
    private String notEquals;
    private String match;
}

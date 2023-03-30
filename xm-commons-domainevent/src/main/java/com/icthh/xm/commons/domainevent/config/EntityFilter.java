package com.icthh.xm.commons.domainevent.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class EntityFilter {

    private String key;
    private Query query;

}

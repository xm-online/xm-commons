package com.icthh.xm.commons.domainevent.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class EntityFilter {

    private String key;
    private Query query;

}

package com.icthh.xm.commons.domainevent.db.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class State {
    private Object previous;
    private Object current;
}

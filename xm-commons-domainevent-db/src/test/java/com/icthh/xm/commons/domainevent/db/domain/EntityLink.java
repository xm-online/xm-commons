package com.icthh.xm.commons.domainevent.db.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityLink {

    private String key;
    private String typeKey;
    private String name;
}

package com.icthh.xm.commons.domainevent.db.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityLocation {
    private String typeKey;
    private Double longitude;
    private Double latitude;
    private String name;
    private String addressLine1;
    private String city;
    private Long xmEntity;
}

package com.icthh.xm.commons.permission.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import java.util.TreeMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(of = "key")
@ToString
@JsonPropertyOrder( {"description", "createdDate", "createdBy", "updatedDate", "updatedBy"})
public class Role implements Comparable<Role> {

    @JsonIgnore
    private String key;
    private String description;
    private String createdDate;
    private String createdBy;
    private String updatedDate;
    private String updatedBy;

    @Override
    public int compareTo(Role o) {
        return key.compareTo(o.getKey());
    }
}

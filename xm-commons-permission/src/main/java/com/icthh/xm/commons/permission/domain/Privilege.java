package com.icthh.xm.commons.permission.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.*;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"description", "resources", "customDescription"})
@ToString
@JsonPropertyOrder({"key", "description"})
public class Privilege implements Comparable<Privilege> {

    @JsonIgnore
    private String msName;
    private String key;
    private Map<String, String> description = new TreeMap<>();
    private Set<String> resources = new TreeSet<>();
    private String customDescription;

    public Privilege() {
    }

    @Override
    public int compareTo(Privilege o) {
        return key.compareTo(o.getKey());
    }
}

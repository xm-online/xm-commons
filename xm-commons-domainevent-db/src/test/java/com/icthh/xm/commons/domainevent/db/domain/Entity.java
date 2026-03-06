package com.icthh.xm.commons.domainevent.db.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Entity {

    private String typeKey;

    private String name;

    private String stateKey;

    private String key;

    private String description;

    private List<EntityLocation> locations;

    private EntityLink link;

    private Map<String, Object> data = new HashMap<>();

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStateKey() {
        return stateKey;
    }

    public void setStateKey(String stateKey) {
        this.stateKey = stateKey;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<EntityLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<EntityLocation> locations) {
        this.locations = locations;
    }

    public EntityLink getLink() {
        return link;
    }

    public void setLink(EntityLink link) {
        this.link = link;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}

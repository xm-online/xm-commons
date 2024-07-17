package com.icthh.xm.commons.flow.domain;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
public class TenantResource implements ConfigWithKey {
    private String key;
    private String resourceType;
    private Map<String, String> name;
    private Map<String, Object> data;
    private Instant updateDate;

    public String getKey() {
        if (key == null) {
            return "";
        }
        return key;
    }

    public String getResourceType() {
        if (resourceType == null) {
            return "";
        }
        return resourceType;
    }

    public Map<String, Object> getData() {
        if (data == null) {
            data = new HashMap<>();
        }
        return data;
    }
}

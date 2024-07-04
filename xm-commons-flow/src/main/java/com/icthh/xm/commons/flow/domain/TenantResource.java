package com.icthh.xm.commons.flow.domain;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class TenantResource implements ConfigWithKey {
    private String key;
    private String resourceType;
    private Map<String, String> name;
    private Map<String, Object> data;
    private Instant updateDate;
}

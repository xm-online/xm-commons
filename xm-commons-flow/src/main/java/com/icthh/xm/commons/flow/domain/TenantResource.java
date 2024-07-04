package com.icthh.xm.commons.flow.domain;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import com.icthh.xm.commons.flow.rest.validator.JsonData;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonData
public class TenantResource implements ConfigWithKey {
    private String key;
    private String resourceType;
    private Map<String, String> name;
    private Map<String, Object> data;
    private Instant updateDate;
}

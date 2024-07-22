package com.icthh.xm.commons.flow.spec.trigger;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import lombok.Data;

import java.util.Map;

@Data
public class TriggerType implements ConfigWithKey {
    private String key;
    private Map<String, Object> name;
    private String configSpec;
    private String configForm;
}

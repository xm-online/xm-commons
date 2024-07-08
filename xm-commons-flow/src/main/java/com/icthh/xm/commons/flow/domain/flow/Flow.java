package com.icthh.xm.commons.flow.domain.flow;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import lombok.Data;

import java.util.List;

@Data
public class Flow implements ConfigWithKey {
    private String key;
    private String version;
    private String description;
    private List<Step> steps;
    private Trigger trigger;
}

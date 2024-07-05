package com.icthh.xm.commons.flow.domain.dto;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import lombok.Data;

import java.util.List;

@Data
public class FlowDto implements ConfigWithKey {
    private String key;
    private String version;
    private List<StepDto> steps;
    private TriggerDto trigger;
}

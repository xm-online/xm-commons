package com.icthh.xm.commons.flow.domain;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class Flow implements ConfigWithKey {
    private String key;
    private String version;
    private String description;
    private String startStep;
    private List<Step> steps;
    private Trigger trigger;

    public List<Step> getSteps() {
        if (steps == null) {
            steps = List.of();
        }
        return steps;
    }
}

package com.icthh.xm.commons.flow.spec.step;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode
public class StepSpec implements ConfigWithKey {

    private String key;
    private Map<String, String> name;
    private String implementation;
    private List<ResourceVariable> resources;
    private String configSpec;
    private String configForm;
    private StepType type;

    @Data
    @EqualsAndHashCode
    public static class ResourceVariable {
        private String key;
        private Map<String, String> name;
        private String resourceType;
    }

    public enum StepType {
        ACTION, CONDITION
    }

}

package com.icthh.xm.commons.flow.spec.step;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.icthh.xm.commons.config.client.api.refreshable.AbstractRefreshableConfiguration;
import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.config.client.utils.Utils.nullSafeList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Component
public class StepSpecService extends AbstractRefreshableConfiguration<Map<String, StepSpec>, List<StepSpec>> {

    public StepSpecService(@Value("${spring.application.name}") String appName,
                           TenantContextHolder tenantContextHolder) {
        super(appName, tenantContextHolder);
    }

    @Override
    public String configName() {
        return "step-spec";
    }

    @Override
    public Map<String, StepSpec> joinTenantConfiguration(List<List<StepSpec>> files) {
        return files.stream()
            .map(nullSafeList())
            .flatMap(List::stream)
            .collect(toMap(ConfigWithKey::getKey, identity()));
    }

    @Override
    public JavaType configFileJavaType(TypeFactory factory) {
        return factory.constructCollectionType(List.class, StepSpec.class);
    }

    public StepSpec getStepSpec(String stepKey) {
        return getStepsSpec().get(stepKey);
    }

    public List<StepSpec> getSteps(StepSpec.StepType stepType) {
        return getStepsSpec().values().stream()
            .filter(stepSpec -> stepType == null || stepSpec.getType() == stepType)
            .collect(Collectors.toList());
    }

    private Map<String, StepSpec> getStepsSpec() {
        Map<String, StepSpec> configuration = getConfiguration();
        configuration = configuration == null ? Map.of() : configuration;
        return configuration;
    }
}

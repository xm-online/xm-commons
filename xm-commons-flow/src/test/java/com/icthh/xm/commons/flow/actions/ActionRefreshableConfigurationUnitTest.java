package com.icthh.xm.commons.flow.actions;

import com.icthh.xm.commons.flow.spec.step.StepSpec;
import com.icthh.xm.commons.flow.spec.step.StepSpecService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActionRefreshableConfigurationUnitTest {

    @Test
    public void testReadConfiguration() {
        TenantContextHolder mock = mock(TenantContextHolder.class);
        when(mock.getTenantKey()).thenReturn("UNIT_TEST");
        var actionRefreshableConfiguration = new StepSpecService("test", mock);
        actionRefreshableConfiguration.onRefresh("/config/tenants/UNIT_TEST/test/step-spec.yml", loadFile("actions/testreadspec.yml"));
        StepSpec actionSpec = actionRefreshableConfiguration.getStepSpec("actionkey");
        assertThat(actionSpec, equalTo(mockAction()));
    }

    private static StepSpec mockAction() {
        var resourceVariable = new StepSpec.ResourceVariable();
        resourceVariable.setKey("userDb");
        resourceVariable.setResourceType("database");
        StepSpec value = new StepSpec();
        value.setKey("actionkey");
        value.setType(StepSpec.StepType.ACTION);
        value.setImplementation("testreadspec");
        value.setResources(List.of(resourceVariable));
        return value;
    }

    @SneakyThrows
    public static String loadFile(String path) {
        try (InputStream cfgInputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(cfgInputStream, UTF_8);
        }
    }

}

package com.icthh.xm.commons.flow.steps;

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

public class StepsRefreshableConfigurationUnitTest {

    @Test
    public void testReadConfiguration() {
        TenantContextHolder mock = mock(TenantContextHolder.class);
        when(mock.getTenantKey()).thenReturn("UNIT_TEST");
        var service = new StepSpecService("test", mock);
        service.onRefresh("/config/tenants/UNIT_TEST/test/flow/step-spec/testreadspec.yml", loadFile("step-spec/testreadspec.yml"));
        StepSpec actionSpec = service.findStepSpec("actionkey");
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

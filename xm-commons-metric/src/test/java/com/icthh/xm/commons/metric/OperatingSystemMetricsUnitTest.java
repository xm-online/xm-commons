package com.icthh.xm.commons.metric;

import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;
import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class OperatingSystemMetricsUnitTest {

    @Test
    void shouldRegisterFreeSwapSpaceSizeAndProcessCpuTimeAsSeparateGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new OperatingSystemMetrics(getOperatingSystemMXBean()).bindTo(registry);

        assertThat(registry.find("freeSwapSpaceSize").gauges()).hasSize(1);
        assertThat(registry.find("processCpuTime").gauges()).hasSize(1);
        assertThat(registry.get("processCpuTime").gauge().value()).isPositive();
    }
}

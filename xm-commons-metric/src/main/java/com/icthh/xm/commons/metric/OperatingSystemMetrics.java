package com.icthh.xm.commons.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.Collections.emptyList;

/**
 * A set of gauges for operating system settings.
 */
public class OperatingSystemMetrics implements MeterBinder {

    private final OperatingSystemMXBean operatingSystemMXBean;

    public OperatingSystemMetrics(OperatingSystemMXBean mxBean) {
        this.operatingSystemMXBean = mxBean;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Set<String> registeredMetrics = getRegisteredGauges(registry);

        if (!registeredMetrics.contains("committedVirtualMemorySize")) {
            Gauge.builder("committedVirtualMemorySize", () -> invokeLong("CommittedVirtualMemorySize"))
                .register(registry);
        }

        if (!registeredMetrics.contains("totalSwapSpaceSize")) {
            Gauge.builder("totalSwapSpaceSize", () -> invokeLong("TotalSwapSpaceSize"))
                .register(registry);
        }

        if (!registeredMetrics.contains("freeSwapSpaceSize")) {
            Gauge.builder("freeSwapSpaceSize", () -> invokeLong("FreeSwapSpaceSize"))
                .register(registry);
        }

        if (!registeredMetrics.contains("freeSwapSpaceSize")) {
            Gauge.builder("freeSwapSpaceSize", () -> invokeLong("ProcessCpuTime"))
                .register(registry);
        }

        if (!registeredMetrics.contains("freePhysicalMemorySize")) {
            Gauge.builder("freePhysicalMemorySize", () -> invokeLong("FreePhysicalMemorySize"))
                .register(registry);
        }

        if (!registeredMetrics.contains("totalPhysicalMemorySize")) {
            Gauge.builder("totalPhysicalMemorySize", () -> invokeLong("TotalPhysicalMemorySize"))
                .register(registry);
        }

        if (!registeredMetrics.contains("fileDescriptor.usage")) {
            Gauge.builder("fileDescriptor.usage", () -> invokeRatio("OpenFileDescriptorCount", "MaxFileDescriptorCount"))
                .register(registry);
        }

        if (!registeredMetrics.contains("systemCpuLoad")) {
            Gauge.builder("systemCpuLoad", () -> invokeDouble("SystemCpuLoad"))
                .register(registry);
        }

        if (!registeredMetrics.contains("processCpuLoad")) {
            Gauge.builder("processCpuLoad", () -> invokeDouble("ProcessCpuLoad"))
                .register(registry);
        }
    }

    private Set<String> getRegisteredGauges(MeterRegistry registry) {
        return registry.getMeters().stream()
            .map(m -> m.getId().getName())
            .collect(Collectors.toSet());
    }

    private Optional<Attribute> getOSAttribute(String attributeName) {
        String[] attributesNames = {attributeName};
        List<Attribute> attributes;
        try {
            attributes = getPlatformMBeanServer().getAttributes(operatingSystemMXBean.getObjectName(), attributesNames)
                .asList();
        } catch (ReflectionException | InstanceNotFoundException ex) {
            attributes = emptyList();
        }
        return attributes.stream().filter(it -> attributeName.equals(it.getName())).findAny();
    }

    private long invokeLong(String attributeName) {
        return getOSAttribute(attributeName).map(value -> (long) value.getValue()).orElse(0L);
    }

    private double invokeDouble(String attributeName) {
        return getOSAttribute(attributeName).map(value -> (double) value.getValue()).orElse(0d);
    }

    private double invokeRatio(String numeratorAttributeName, String denominatorAttributeName) {
        Optional<Long> numerator = getOSAttribute(numeratorAttributeName).map(it -> (long) it.getValue());
        Optional<Long> denominator = getOSAttribute(denominatorAttributeName).map(it -> (long) it.getValue())
                                                                             .filter(it -> it != 0);

        return numerator.map(numValue -> denominator.map(denValue -> 1.0 * numValue / denValue)
                        .orElse(Double.NaN))
                        .orElse(Double.NaN);
    }
}

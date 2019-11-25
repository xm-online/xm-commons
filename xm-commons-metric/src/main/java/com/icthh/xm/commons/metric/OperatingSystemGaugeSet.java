package com.icthh.xm.commons.metric;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.Collections.emptyList;

/**
 * A set of gauges for operating system settings.
 */
public class OperatingSystemGaugeSet implements MetricSet {

    private final OperatingSystemMXBean operatingSystemMXBean;

    public OperatingSystemGaugeSet(OperatingSystemMXBean mxBean) {
        this.operatingSystemMXBean = mxBean;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put("committedVirtualMemorySize", (Gauge<Long>) () -> invokeLong("CommittedVirtualMemorySize"));
        gauges.put("totalSwapSpaceSize", (Gauge<Long>) () -> invokeLong("TotalSwapSpaceSize"));
        gauges.put("freeSwapSpaceSize", (Gauge<Long>) () -> invokeLong("FreeSwapSpaceSize"));
        gauges.put("processCpuTime", (Gauge<Long>) () -> invokeLong("ProcessCpuTime"));
        gauges.put("freePhysicalMemorySize", (Gauge<Long>) () -> invokeLong("FreePhysicalMemorySize"));
        gauges.put("totalPhysicalMemorySize", (Gauge<Long>) () -> invokeLong("TotalPhysicalMemorySize"));
        gauges.put("fileDescriptor.usage", (Gauge<Double>) () -> invokeRatio("OpenFileDescriptorCount", "MaxFileDescriptorCount"));
        gauges.put("systemCpuLoad", (Gauge<Double>) () -> invokeDouble("SystemCpuLoad"));
        gauges.put("processCpuLoad", (Gauge<Double>) () -> invokeDouble("ProcessCpuLoad"));

        return gauges;
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

package com.icthh.xm.commons.metric;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        gauges.put("committedVirtualMemorySize",
            (Gauge<Long>) () -> invokeLong("getCommittedVirtualMemorySize"));
        gauges.put("totalSwapSpaceSize", (Gauge<Long>) () -> invokeLong("getTotalSwapSpaceSize"));
        gauges.put("freeSwapSpaceSize", (Gauge<Long>) () -> invokeLong("getFreeSwapSpaceSize"));
        gauges.put("processCpuTime", (Gauge<Long>) () -> invokeLong("getProcessCpuTime"));
        gauges.put("freePhysicalMemorySize", (Gauge<Long>) () -> invokeLong("getFreePhysicalMemorySize"));
        gauges.put("totalPhysicalMemorySize", (Gauge<Long>) () -> invokeLong("getTotalPhysicalMemorySize"));
        gauges.put("fileDescriptor.usage",
            (Gauge<Double>) () -> invokeRatio("getOpenFileDescriptorCount",
                "getMaxFileDescriptorCount"));
        gauges.put("systemCpuLoad", (Gauge<Double>) () -> invokeDouble("getSystemCpuLoad"));
        gauges.put("processCpuLoad", (Gauge<Double>) () -> invokeDouble("getProcessCpuLoad"));

        return gauges;
    }

    private Optional<Method> getMethod(String name) {
        try {
            final Method method = operatingSystemMXBean.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return Optional.of(method);
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    private long invokeLong(String methodName) {
        Optional<Method> method = getMethod(methodName);
        if (method.isPresent()) {
            try {
                return (long) method.get().invoke(operatingSystemMXBean);
            } catch (IllegalAccessException | InvocationTargetException ite) {
                return 0L;
            }
        }
        return 0L;
    }

    private double invokeDouble(String methodName) {
        Optional<Method> method = getMethod(methodName);
        if (method.isPresent()) {
            try {
                return (double) method.get().invoke(operatingSystemMXBean);
            } catch (IllegalAccessException | InvocationTargetException ite) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private double invokeRatio(String numeratorMethodName, String denominatorMethodName) {
        Optional<Method> numeratorMethod = getMethod(numeratorMethodName);
        Optional<Method> denominatorMethod = getMethod(denominatorMethodName);
        if (numeratorMethod.isPresent() && denominatorMethod.isPresent()) {
            try {
                long numerator = (long) numeratorMethod.get().invoke(operatingSystemMXBean);
                long denominator = (long) denominatorMethod.get().invoke(operatingSystemMXBean);
                if (0 == denominator) {
                    return Double.NaN;
                }
                return 1.0 * numerator / denominator;
            } catch (IllegalAccessException | InvocationTargetException ite) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

}

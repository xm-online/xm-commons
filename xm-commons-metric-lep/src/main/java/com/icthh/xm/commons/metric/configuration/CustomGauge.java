package com.icthh.xm.commons.metric.configuration;

import lombok.Data;

@Data
public class CustomGauge {
    private String name;
    private Integer updatePeriodSeconds;
}

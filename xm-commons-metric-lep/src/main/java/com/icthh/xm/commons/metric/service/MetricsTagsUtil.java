package com.icthh.xm.commons.metric.service;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.Map;


public final class MetricsTagsUtil {

    public static Tags toTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Tags.empty();
        }
        return Tags.of(tags.entrySet().stream()
            .map(e -> Tag.of(e.getKey(), e.getValue()))
            .toList());
    }
}

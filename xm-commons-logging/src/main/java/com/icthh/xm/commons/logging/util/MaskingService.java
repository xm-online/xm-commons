package com.icthh.xm.commons.logging.util;

import com.icthh.xm.commons.logging.config.LoggingConfig.MaskingLogConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;

public class MaskingService {
    private final Pattern multilinePattern;
    private final boolean enabled;

    public MaskingService(MaskingLogConfiguration maskingLogConfiguration, List<String> defaultMaskPatterns) {
        List<String> maskPatterns = getMaskPatterns(maskingLogConfiguration);
        maskPatterns.addAll(defaultMaskPatterns);
        this.enabled = isEnabled(maskingLogConfiguration);
        this.multilinePattern = Pattern.compile(String.join("|", maskPatterns), Pattern.MULTILINE);
    }

    private static List<String> getMaskPatterns(MaskingLogConfiguration maskingLogConfiguration) {
        List<String> patterns = Optional.ofNullable(maskingLogConfiguration)
            .map(MaskingLogConfiguration::getMaskPatterns)
            .orElse(emptyList());
        return new ArrayList<>(patterns);
    }

    private static Boolean isEnabled(MaskingLogConfiguration maskingLogConfiguration) {
        return Optional.ofNullable(maskingLogConfiguration)
            .map(MaskingLogConfiguration::getEnabled)
            .orElse(false);
    }

    public String maskMessage(String message) {
        if (!enabled) {
            return message;
        }

        StringBuilder sb = new StringBuilder(message);
        Matcher matcher = multilinePattern.matcher(sb);
        while (matcher.find()) {
            IntStream.rangeClosed(1, matcher.groupCount()).forEach(group -> {
                if (matcher.group(group) != null) {
                    IntStream.range(matcher.start(group), matcher.end(group)).forEach(i -> sb.setCharAt(i, '*'));
                }
            });
        }
        return sb.toString();
    }
}

package com.icthh.xm.commons.config.client.repository.message;

import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigPatternRequest {
    private Collection<String> patternPaths;
    private String version;
}

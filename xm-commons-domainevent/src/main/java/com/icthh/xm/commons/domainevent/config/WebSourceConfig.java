package com.icthh.xm.commons.domainevent.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSourceConfig extends SourceConfig {

    private List<TransformMappingConfig> transform;
    private List<FilterConfig> filter;
    private Set<String> headers = Set.of("cookie", "authorization");
}

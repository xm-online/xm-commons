package com.icthh.xm.commons.config.client.api;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Getter
@Component
public class FetchConfigurationSettings {

    private final List<String> msConfigPatterns;
    private final Boolean isFetchAll;

    public FetchConfigurationSettings(@Value("${spring.application.name}") String applicationName,
                                      @Value("${application.config-fetch-all.enabled:false}") Boolean isFetchAll) {
        // Config keys use "/", while paths of the file mode on Windows use "\", so both forms are needed.
        List<String> unixPatterns = List.of(
            "/config/tenants/commons/**",
            "/config/tenants/*",
            "/config/tenants/{tenantName}/commons/**",
            "/config/tenants/{tenantName}/*",
            "/config/tenants/{tenantName}/" + applicationName + "/**",
            "/config/tenants/{tenantName}/config/**"
        );
        this.msConfigPatterns = Stream.concat(
            unixPatterns.stream(),
            unixPatterns.stream().map(FilenameUtils::separatorsToWindows)
        ).toList();
        this.isFetchAll = isFetchAll;
    }

}

package com.icthh.xm.commons.config.client.api;


import java.io.File;
import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class FetchConfigurationSettings {

    private final List<String> msConfigPatterns;
    private final Boolean isFetchAll;

    public FetchConfigurationSettings(@Value("${spring.application.name}") String applicationName,
                                      @Value("${application.config-fetch-all.enabled:false}") Boolean isFetchAll) {
        String sep = File.separator;
        this.msConfigPatterns = List.of(
            sep + "config" + sep + "tenants" + sep + "commons" + sep + "**",
            sep + "config" + sep + "tenants" + sep + "*",
            sep + "config" + sep + "tenants" + sep + "{tenantName}" + sep + "commons" + sep + "**",
            sep + "config" + sep + "tenants" + sep + "{tenantName}" + sep + "*",
            sep + "config" + sep + "tenants" + sep + "{tenantName}" + sep + applicationName + sep + "**",
            sep + "config" + sep + "tenants" + sep + "{tenantName}" + sep + "config" + sep + "**"
        );
        this.isFetchAll = isFetchAll;
    }

}

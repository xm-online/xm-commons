package com.icthh.xm.commons.config.client.api;


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
        this.msConfigPatterns = List.of(
                "/config/tenants/commons/**",
                "/config/tenants/*",
                "/config/tenants/{tenantName}/commons/**",
                "/config/tenants/{tenantName}/*",
                "/config/tenants/{tenantName}/" + applicationName + "/**",
                "/config/tenants/{tenantName}/config/**");
        this.isFetchAll = isFetchAll;
    }

}

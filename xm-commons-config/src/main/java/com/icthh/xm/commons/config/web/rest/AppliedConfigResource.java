package com.icthh.xm.commons.config.web.rest;

import com.icthh.xm.commons.config.client.applied.AppliedConfigResponse;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty("xm-config.enabled")
@ConditionalOnProperty(name = "application.applied-config.enabled", havingValue = "true", matchIfMissing = true)
public class AppliedConfigResource {

    private final AppliedConfigService appliedConfigService;

    @GetMapping("/applied-config")
    @PreAuthorize("hasPermission({'tenant': #tenant}, 'APPLIED_CONFIG.GET')")
    @LoggingAspectConfig(resultDetails = false)
    public AppliedConfigResponse get(@RequestParam(required = false) String tenant,
                                     @RequestParam(defaultValue = "false") boolean details) {
        return appliedConfigService.getAppliedConfig(tenant, details);
    }
}

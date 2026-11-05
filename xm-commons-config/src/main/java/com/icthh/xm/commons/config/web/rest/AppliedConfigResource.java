package com.icthh.xm.commons.config.web.rest;

import com.icthh.xm.commons.config.client.applied.AppliedConfigResponse;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService.Aggregate;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService.PathHash;
import com.icthh.xm.commons.config.client.applied.AppliedConfigVersion;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.internal.SpringSecurityXmAuthenticationContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.icthh.xm.commons.config.client.applied.AppliedConfigService.COMMONS;

/**
 * Applied config version API. Access is decided by {@code APPLIED_CONFIG.GET}; what the caller sees is scoped here:
 * without the management role only the tenant of the token and {@code commons}.
 */
@RestController
@RequestMapping("/api")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty("xm-config.enabled")
@ConditionalOnProperty(name = "application.applied-config.enabled", havingValue = "true", matchIfMissing = true)
public class AppliedConfigResource {

    private final AppliedConfigService service;
    private final XmAuthenticationContextHolder authContextHolder;
    private final String managementRole;

    public AppliedConfigResource(AppliedConfigService service,
                                 @Value("${application.management.access.role:SYSTEM-ADMIN}") String managementRole) {
        this.service = service;
        this.authContextHolder = new SpringSecurityXmAuthenticationContextHolder();
        this.managementRole = managementRole;
    }

    @GetMapping("/applied-config")
    @PreAuthorize("hasPermission({'tenant': #tenant}, 'APPLIED_CONFIG.GET')")
    @PrivilegeDescription("Privilege to get the applied config version and config hashes of the service")
    @LoggingAspectConfig(resultDetails = false)
    public AppliedConfigResponse get(@RequestParam(required = false) String tenant,
                                     @RequestParam(defaultValue = "false") boolean details) {
        XmAuthenticationContext context = authContextHolder.getContext();
        boolean management = context.getAuthorities().stream()
            .anyMatch(it -> managementRole.equals(it.getAuthority()));
        String ownTenant = context.getTenantName().map(this::upper).orElse(null);
        String requested = tenant == null ? ownTenant : upper(tenant);
        if (!management && (ownTenant == null || !ownTenant.equals(requested))) {
            throw new AccessDeniedException("Applied config of another tenant is not available");
        }
        return buildResponse(management, tenant != null, requested, details);
    }

    private AppliedConfigResponse buildResponse(boolean management, boolean tenantRequested, String tenant,
                                                boolean details) {
        List<PathHash> all = service.getPathHashes();
        boolean allKeys = management && !tenantRequested;
        List<PathHash> inScope = allKeys ? all : all.stream().filter(it -> inTenantScope(it, tenant)).toList();

        Map<String, Aggregate> hashes = allKeys ? AppliedConfigService.aggregatesByTenantKey(all)
            : tenantAggregates(all, tenant);
        AppliedConfigVersion commit = management ? service.getVersion() : service.getVersion().forTenant(tenant);
        Aggregate total = management ? AppliedConfigService.aggregateOf(all) : null;
        return new AppliedConfigResponse(commit, service.getFetchScope(), hashes, total, details ? inScope : null);
    }

    private Map<String, Aggregate> tenantAggregates(List<PathHash> all, String tenant) {
        Map<String, Aggregate> result = new TreeMap<>();
        result.put(tenant, AppliedConfigService.aggregateOf(AppliedConfigService.filterByKey(all, tenant)));
        result.put(COMMONS, AppliedConfigService.aggregateOf(AppliedConfigService.filterByKey(all, COMMONS)));
        return result;
    }

    private boolean inTenantScope(PathHash pathHash, String tenant) {
        String key = AppliedConfigService.tenantKeyOf(pathHash.path());
        return key.equalsIgnoreCase(tenant) || COMMONS.equals(key);
    }

    private String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }
}

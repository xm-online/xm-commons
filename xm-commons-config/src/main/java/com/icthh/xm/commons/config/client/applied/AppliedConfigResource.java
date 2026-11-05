package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.config.client.applied.AppliedConfigService.Aggregate;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService.PathHash;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.security.internal.SpringSecurityXmAuthenticationContextHolder;
import com.icthh.xm.commons.security.spring.config.SecurityConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.icthh.xm.commons.config.client.applied.AppliedConfigService.COMMONS;

/**
 * Handler of {@code GET /api/applied-config}. It is not a stereotype bean: it is routed by a
 * {@code RouterFunction}, so a component scan of services cannot register it a second time.
 *
 * <p>Access is checked here as well as by the URL rule in {@link SecurityConfiguration}, because services with
 * their own filter chain do not have that rule. Order: authenticated, then management role or the privilege,
 * then tenant scope.
 */
public class AppliedConfigResource {

    private final AppliedConfigService service;
    private final XmAuthenticationContextHolder authContextHolder;
    private final ObjectProvider<XmPermissionCheckService> permissionCheckService;
    private final String managementRole;

    public AppliedConfigResource(AppliedConfigService service,
                                 ObjectProvider<XmAuthenticationContextHolder> authContextHolder,
                                 ObjectProvider<XmPermissionCheckService> permissionCheckService,
                                 String managementRole) {
        this.service = service;
        this.authContextHolder = authContextHolder.getIfAvailable(SpringSecurityXmAuthenticationContextHolder::new);
        this.permissionCheckService = permissionCheckService;
        this.managementRole = managementRole;
    }

    public ServerResponse get(ServerRequest request) {
        XmAuthenticationContext context = authContextHolder.getContext();
        if (!context.isFullyAuthenticated()) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean management = context.getAuthorities().stream()
            .anyMatch(it -> managementRole.equals(it.getAuthority()));
        if (!management && !hasPrivilege()) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).build();
        }
        String ownTenant = context.getTenantName().map(this::upper).orElse(null);
        String requested = request.param("tenant").map(this::upper).orElse(ownTenant);
        if (!management && (ownTenant == null || !ownTenant.equals(requested))) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).build();
        }
        boolean details = request.param("details").map(Boolean::parseBoolean).orElse(false);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
            .body(buildResponse(management, request.param("tenant").isPresent(), requested, details));
    }

    private boolean hasPrivilege() {
        XmPermissionCheckService checkService = permissionCheckService.getIfAvailable();
        return checkService != null && checkService.hasPermission(SecurityConfiguration.APPLIED_CONFIG_PRIVILEGE);
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

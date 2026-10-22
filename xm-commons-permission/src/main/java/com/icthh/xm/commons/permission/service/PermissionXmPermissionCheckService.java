package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.security.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

/**
 * The {@link PermissionXmPermissionCheckService} class.
 *
 * <p>Checks privileges by {@link PermissionCheckService}. The security filter chain runs before
 * the tenant context is set, so without the tenant context the tenant from the token is used.
 */
@Service
@RequiredArgsConstructor
public class PermissionXmPermissionCheckService implements XmPermissionCheckService {

    private final PermissionCheckService permissionCheckService;
    private final XmAuthenticationContextHolder authContextHolder;
    private final TenantContextHolder tenantContextHolder;

    @Override
    public boolean hasPermission(String privilege, Map<String, Object> context) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        if (isSuperAdmin(authentication)) {
            return true;
        }
        return getTenant()
            .map(tenant -> hasPermissionInTenant(tenant, authentication, privilege, context))
            .orElse(false);
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(SUPER_ADMIN::equals);
    }

    private Optional<String> getTenant() {
        Optional<String> currentTenant = TenantContextUtils.getTenantKey(tenantContextHolder).map(TenantKey::getValue);
        if (currentTenant.isPresent()) {
            return currentTenant;
        }
        XmAuthenticationContext authContext = authContextHolder.getContext();
        return authContext.isFullyAuthenticated() ? authContext.getTenantName() : Optional.empty();
    }

    private boolean hasPermissionInTenant(String tenant, Authentication authentication, String privilege,
                                          Map<String, Object> context) {
        try {
            return tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant),
                () -> permissionCheckService.hasPermission(authentication, context, privilege));
        } catch (AccessDeniedException e) {
            return false;
        }
    }
}

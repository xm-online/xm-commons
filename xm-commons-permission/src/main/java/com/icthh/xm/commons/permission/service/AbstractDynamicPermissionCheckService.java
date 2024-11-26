package com.icthh.xm.commons.permission.service;

import com.google.common.base.Preconditions;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.icthh.xm.commons.permission.utils.SecurityUtils.getRoleKeyOrNull;
import static com.icthh.xm.commons.permission.utils.SecurityUtils.getUserKeyOrNull;
import static java.lang.String.format;

@Slf4j
@RequiredArgsConstructor //todo add default implementation !!!
public abstract class AbstractDynamicPermissionCheckService implements DynamicPermissionCheckService {

    private final PermissionCheckService permissionCheckService;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;

    /**
     * Checks if user has permission with dynamic key feature
     * If some feature defined by IFeatureContext in tenantConfigService enabled TRUE, then check
     * by @checkContextPermission applied P('XXX'.'YYY')
     * Otherwise basePermission evaluated only
     * @param featureContext    feature context
     * @param basePermission    base permission 'XXX'
     * @param suffix            context permission 'YYY'
     * @return result from PermissionCheckService.hasPermission
     */
    @IgnoreLogginAspect
    @Override
    public boolean checkContextPermission(IFeatureContext featureContext, String basePermission, String suffix) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(basePermission));
        Preconditions.checkArgument(StringUtils.isNotEmpty(suffix));
        if (featureContext.isEnabled(this)) {
            return checkContextPermission(basePermission, suffix);
        }
        return assertPermission(basePermission);
    }

    /**
     * Checks if user has permission with dynamic key feature permission = basePermission + "." + suffix
     * @param basePermission    base permission
     * @param suffix            suffix
     * @return result from PermissionCheckService.hasPermission(permission) from assertPermission
     */
    private boolean checkContextPermission(String basePermission, String suffix) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(basePermission));
        Preconditions.checkArgument(StringUtils.isNotEmpty(suffix));
        final String permission = basePermission + "." + suffix;
        return assertPermission(permission);
    }

    /**
     * Assert permission via permissionCheckService.hasPermission
     * @param permission    Permission
     */
    private boolean assertPermission(final String permission) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(permission));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean permitted = permissionCheckService
            .hasPermission(authentication, permission);

        if (!permitted) {
            String msg = format("access denied: privilege=%s, roleKey=%s, user=%s due to privilege is not permitted",
                permission, getRoleKeyOrNull(authentication), getUserKeyOrNull(xmAuthenticationContextHolder));
            throw new AccessDeniedException(msg);
        }
        return true;
    }
}

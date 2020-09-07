package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.exceptions.SkipPermissionException;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.access.ResourceFactory;
import com.icthh.xm.commons.permission.access.subject.Subject;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.permission.domain.EnvironmentVariable;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.ReactionStrategy;
import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.event.Level;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.expression.OAuth2SecurityExpressionMethods;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@IgnoreLogginAspect
public class PermissionCheckService {

    private static final String ERROR_ROLE_IS_UNDEFINED = "Role is undefined";
    private static final String LOG_KEY = "log";

    private static final Method GET_REQUEST_HEADER = lookupGetRequestHeaderMethod();

    private final TenantContextHolder tenantContextHolder;
    private final PermissionService permissionService;
    private final ResourceFactory resourceFactory;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final RoleService roleService;

    /**
     * Check permission for role and privilege key only.
     * @param authentication the authentication
     * @param privilege the privilege key
     * @return true if permitted
     */
    public boolean hasPermission(Authentication authentication,
                                 Object privilege) {
        return checkRole(authentication, privilege, true)
            || checkPermission(authentication, null, privilege, false, true);
    }

    /**
     * Check permission for role, privilege key and resource condition.
     * @param authentication the authentication
     * @param resource the resource
     * @param privilege the privilege key
     * @return true if permitted
     */
    public boolean hasPermission(Authentication authentication,
                                 Object resource,
                                 Object privilege) {
        boolean logPermission = isLogPermission(resource);
        return checkRole(authentication, privilege, logPermission)
            || checkPermission(authentication, resource, privilege, true, logPermission);
    }

    /**
     * Check permission for role, privilege key, new resource and old resource.
     * @param authentication the authentication
     * @param resource the old resource
     * @param resourceType the resource type
     * @param privilege the privilege key
     * @return true if permitted
     */
    @SuppressWarnings("unchecked")
    public boolean hasPermission(Authentication authentication,
                                 Serializable resource,
                                 String resourceType,
                                 Object privilege) {
        boolean logPermission = isLogPermission(resource);
        if (checkRole(authentication, privilege, logPermission)) {
            return true;
        }
        if (resource != null) {
            Object resourceId = ((Map<String, Object>) resource).get("id");
            if (resourceId != null) {
                ((Map<String, Object>) resource).put(resourceType,
                    resourceFactory.getResource(resourceId, resourceType));
            }
        }
        return checkPermission(authentication, resource, privilege, true, logPermission);
    }

    /**
     * Create condition with replaced subject variables.
     *
     * <p>SpEL condition translated to SQL condition with replacing #returnObject to returnObject
     * and enriching #subject.* from Subject object (see {@link Subject}).
     *
     * <p>As an option, SpEL could be translated to SQL
     * via {@link SpelExpression} method {@code getAST()}
     * with traversing through {@link SpelNode} nodes and building SQL expression.
     *
     * @param authentication the authentication
     * @param privilegeKey the privilege key
     * @param translator the spel translator
     * @return condition if permitted, or null
     */
    public String createCondition(Authentication authentication, Object privilegeKey, SpelTranslator translator) {
        if (!hasPermission(authentication, privilegeKey)) {
            throw new AccessDeniedException("Access is denied");
        }

        String roleKey = getRoleKey(authentication);

        Permission permission = getPermission(roleKey, privilegeKey);

        Subject subject = getSubject(roleKey);

        if (!RoleConstant.SUPER_ADMIN.equals(roleKey)
            && permission != null && permission.getResourceCondition() != null) {
            return translator.translate(permission.getResourceCondition().getExpressionString(), subject);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private boolean checkPermission(Authentication authentication,
                                    Object resource,
                                    Object privilegeKey,
                                    boolean checkCondition,
                                    boolean logPermission) {
        String roleKey = getRoleKey(authentication);
        Map<String, Object> resources = new HashMap<>();

        if (resource != null) {
            resources.putAll((Map<String, Object>) resource);
        }
        resources.put("subject", getSubject(roleKey));
        resources.put("oauth2", new OAuth2SecurityExpressionMethods(authentication));

        Map<String, String> env = new HashMap<>();
        env.put(EnvironmentVariable.IP.getName(), xmAuthenticationContextHolder
            .getContext().getRemoteAddress().orElse(null));
        resources.put("env", env);//put some env variables in this map

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(resources);
        context.registerFunction("getRequestHeader", GET_REQUEST_HEADER);

        Permission permission = getPermission(roleKey, privilegeKey);

        if (!isPermissionValid(permission)) {
            log(logPermission, Level.ERROR,
                "access denied: privilege={}, role={}, userKey={} due to privilege is not permitted",
                privilegeKey, roleKey, getUserKey());
            return false;
        }
        boolean validCondition = true;
        if (!isConditionValid(permission.getEnvCondition(), context)) {
            log(logPermission, Level.ERROR,
                "access denied: privilege={}, role={}, userKey={} due to env condition: [{}] with context [{}]",
                privilegeKey, roleKey, getUserKey(), permission.getEnvCondition().getExpressionString(), resources);
            validCondition = false;
        }
        if (checkCondition && !isConditionValid(permission.getResourceCondition(), context)) {
            log(logPermission, Level.ERROR,
                "access denied: privilege={}, role={}, userKey={} due to env condition: [{}] with context [{}] "
                    + "with context [{}]",
                privilegeKey, roleKey, getUserKey(), permission.getResourceCondition().getExpressionString(),
                resources);
            validCondition = false;
        }
        if (!validCondition && ReactionStrategy.SKIP.equals(permission.getReactionStrategy())) {
            throw new SkipPermissionException("Skip permission", permission.getRoleKey() + ":"
                + permission.getPrivilegeKey());
        } else if (!validCondition) {
            return false;
        }
        log(logPermission, Level.INFO,
            "access granted: privilege={}, role={}, userKey={}",
            privilegeKey, roleKey, getUserKey());
        return true;
    }

    private boolean checkRole(Authentication authentication, Object privilege, boolean logPermission) {
        String roleKey = getRoleKey(authentication);

        if (RoleConstant.SUPER_ADMIN.equals(roleKey)) {
            log(logPermission, Level.INFO,
                "access granted: privilege={}, role=SUPER-ADMIN, userKey={}",
                privilege, getUserKey());
            return true;
        }

        if (!roleService.getRoles(TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext()))
            .containsKey(roleKey)) {
            log(logPermission, Level.ERROR,
                "access denied: privilege={}, role={}, userKey={} due to role is missing",
                privilege, roleKey, getUserKey());
            throw new AccessDeniedException("Access is denied");
        }

        return false;
    }

    private static boolean isConditionValid(Expression expression, StandardEvaluationContext context) {
        boolean result;
        if (expression == null || StringUtils.isEmpty(expression.getExpressionString())) {
            result = true;
        } else {
            try {
                result = expression.getValue(context, Boolean.class);
            } catch (Exception e) {
                result = false;
                log.error("Exception while getting value ", e);
            }
        }
        return result;
    }

    private static boolean isPermissionValid(Permission permission) {
        boolean result = true;
        if (permission == null || permission.isDisabled()) {
            result = false;
        }
        return result;
    }

    private Subject getSubject(String roleKey) {
        XmAuthenticationContext authContext = xmAuthenticationContextHolder.getContext();
        return new Subject(authContext.getLogin().orElse(null),
            authContext.getUserKey().orElse(null), roleKey);
    }

    private String getUserKey() {
        return xmAuthenticationContextHolder.getContext().getUserKey().orElse(null);
    }

    private static String getRoleKey(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException(ERROR_ROLE_IS_UNDEFINED))
            .getAuthority();
    }

    private Permission getPermission(String roleKey, Object privilegeKey) {
        return permissionService.getPermissions(
            TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext()))
            .get(roleKey + ":" + privilegeKey);
    }

    @SuppressWarnings("unchecked")
    private static boolean isLogPermission(Object resource) {
        if (resource != null && resource instanceof Map) {
            Map<String, Object> resourceMap = (Map<String, Object>) resource;
            Object logFlag = resourceMap.get(LOG_KEY);
            if (logFlag != null && logFlag instanceof Boolean) {
                resourceMap.remove(LOG_KEY);
                return (Boolean) logFlag;
            }
        }
        return true;
    }

    private static void log(boolean allowToLog, Level logLevel, String logMessage, Object... logArgs) {
        if (!allowToLog) {
            return;
        }
        switch (logLevel) {
            case INFO:
                log.info(logMessage, logArgs);
                break;
            case ERROR:
                log.error(logMessage, logArgs);
                break;
            default:
                break;
        }
    }

    @SneakyThrows
    private static Method lookupGetRequestHeaderMethod() {
        return RequestHeaderUtils.class.getDeclaredMethod("getRequestHeader", String.class);
    }

    private static class RequestHeaderUtils {
        public static String getRequestHeader(String headerName) {
            return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader(headerName))
                .orElse(null);
        }
    }
}

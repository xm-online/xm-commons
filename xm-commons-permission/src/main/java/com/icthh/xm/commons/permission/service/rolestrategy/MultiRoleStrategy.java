package com.icthh.xm.commons.permission.service.rolestrategy;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.commons.permission.utils.CollectionUtils.listsNotEqualsIgnoreOrder;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.icthh.xm.commons.exceptions.SkipPermissionException;
import com.icthh.xm.commons.permission.access.ResourceFactory;
import com.icthh.xm.commons.permission.access.subject.Subject;
import com.icthh.xm.commons.permission.domain.EnvironmentVariable;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.ReactionStrategy;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import com.icthh.xm.commons.permission.utils.RequestHeaderUtils;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.expression.OAuth2SecurityExpressionMethods;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component("multiRoleStrategy")
public class MultiRoleStrategy implements RoleStrategy {

    private static final String LOG_KEY = "log";
    private static final Method GET_REQUEST_HEADER = lookupGetRequestHeaderMethod();

    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final TenantContextHolder tenantContextHolder;
    private final PermissionService permissionService;
    private final ResourceFactory resourceFactory;
    private final RoleService roleService;

    /**
     * Check permission for role and privilege key only.
     *
     * @param authentication the authentication
     * @param privilege the privilege key
     * @return true if permitted
     */
    public boolean hasPermission(Authentication authentication, Object privilege) {
        return checkRole(authentication, privilege, true)
            || checkPermission(authentication, null, privilege, false, true);
    }

    /**
     * Check permission for role, privilege key and resource condition.
     *
     * @param authentication the authentication
     * @param resource the resource
     * @param privilege the privilege key
     * @return true if permitted
     */
    public boolean hasPermission(
        Authentication authentication,
        Object resource,
        Object privilege
    ) {
        boolean logPermission = isLogPermission(resource);
        return checkRole(authentication, privilege, logPermission)
            || checkPermission(authentication, resource, privilege, true, logPermission);
    }

    /**
     * Check permission for role, privilege key, new resource and old resource.
     *
     * @param authentication the authentication
     * @param resource the old resource
     * @param resourceType the resource type
     * @param privilege the privilege key
     * @return true if permitted
     */
    @SuppressWarnings("unchecked")
    public boolean hasPermission(
        Authentication authentication,
        Serializable resource,
        String resourceType,
        Object privilege
    ) {
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
     * via {@link SpelExpression} method {@code getAST()} with traversing through {@link SpelNode} nodes and building SQL
     * expression.
     *
     * @param authentication the authentication
     * @param privilegeKey the privilege key
     * @param translator the spel translator
     * @return condition if permitted, or null
     */
    public String createCondition(
        Authentication authentication, Object privilegeKey, SpelTranslator translator
    ) {
        if (!hasPermission(authentication, privilegeKey)) {
            throw new AccessDeniedException("Access is denied");
        }

        Collection<String> roleKeys = getRoleKeys(authentication);

        if (roleKeys.contains(SUPER_ADMIN)) {
            return EMPTY;
        }

        Collection<Permission> permissions = getPermissions(roleKeys, privilegeKey);
        Map<String, Subject> subjects = getSubjects(roleKeys);

        if (permissions.size() > 1) {
            return permissions.stream()
                .filter(permission -> nonNull(permission.getResourceCondition()))
                .map(permission -> translator
                    .translate(permission.getResourceCondition().getExpressionString(), subjects.get(permission.getRoleKey())))
                .map(condition -> format("(%s)", condition))
                .reduce(" OR ", String::concat);
        }

        return permissions.stream()
            .filter(permission -> nonNull(permission.getResourceCondition()))
            .map(permission -> translator
                .translate(permission.getResourceCondition().getExpressionString(), subjects.get(permission.getRoleKey())))
            .map(condition -> format("(%s)", condition))
            .reduce("", String::concat);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    boolean checkPermission(
        Authentication authentication,
        Object resource,
        Object privilegeKey,
        boolean checkCondition,
        boolean logPermission
    ) {
        Collection<String> roleKey = getRoleKeys(authentication);
        Map<String, Object> resources = new HashMap<>();

        if (resource != null) {
            resources.putAll((Map<String, Object>) resource);
        }

        resources.put("subject", getSubjects(roleKey).values());
        resources.put("oauth2", new OAuth2SecurityExpressionMethods(authentication));

        Map<String, String> env = new HashMap<>();
        env.put(
            EnvironmentVariable.IP.getName(),
            xmAuthenticationContextHolder.getContext().getRemoteAddress().orElse(null)
        );
        resources.put("env", env);

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(resources);
        context.registerFunction("getRequestHeader", GET_REQUEST_HEADER);

        Collection<Permission> permissions = getPermissions(roleKey, privilegeKey);

        if (!isPermissionEnabled(permissions)) {
            log(logPermission,
                Level.ERROR,
                "access denied: privilege={}, role={}, userKey={} due to privilege is not permitted",
                privilegeKey,
                roleKey,
                getUserKey()
            );
            return false;
        }

        boolean validCondition = true;

        if (!isConditionValid(permissions, context, permission -> permission.getEnvCondition())) {
            log(logPermission,
                Level.ERROR,
                "access denied: privilege={}, role={}, userKey={} due to env condition: {} with context [{}]",
                privilegeKey,
                roleKey,
                getUserKey(),
                permissions.stream()
                    .map(Permission::getEnvCondition)
                    .filter(Objects::nonNull)
                    .map(Expression::getExpressionString)
                    .collect(toList()),
                resources
            );
            validCondition = false;
        }

        if (checkCondition && !isConditionValid(permissions, context, permission -> permission.getResourceCondition())) {
            log(logPermission,
                Level.ERROR,
                "access denied: privilege={}, role={}, userKey={} due to resource condition: {} with context [{}] ",
                privilegeKey,
                roleKey,
                getUserKey(),
                permissions.stream()
                    .map(Permission::getResourceCondition)
                    .filter(Objects::nonNull)
                    .map(Expression::getExpressionString)
                    .collect(toList()),
                resources
            );
            validCondition = false;
        }

        List<ReactionStrategy> reactionStrategies = permissions.stream()
            .map(Permission::getReactionStrategy)
            .filter(Objects::nonNull)
            .collect(toList());

        if (!validCondition && reactionStrategies.contains(ReactionStrategy.SKIP)) {
            throw new SkipPermissionException(
                "Skip permission",
                permissions.stream()
                    .map(permission -> permission.getRoleKey() + ":" + permission.getPrivilegeKey())
                    .collect(toList())
            );
        } else if (!validCondition) {
            return false;
        }

        log(logPermission,
            Level.INFO,
            "access granted: privilege={}, role={}, userKey={}",
            privilegeKey,
            roleKey,
            getUserKey()
        );
        return true;
    }

    private boolean checkRole(Authentication authentication, Object privilege, boolean logPermission) {
        Collection<String> roleKeys = getRoleKeys(authentication);

        if (roleKeys.contains(SUPER_ADMIN)) {
            log(logPermission,
                Level.INFO,
                "access granted: privilege={}, role=SUPER-ADMIN, userKey={}",
                privilege,
                getUserKey()
            );
            return true;
        }

        if (listsNotEqualsIgnoreOrder(
            roleService.getRoles(getRequiredTenantKeyValue(tenantContextHolder.getContext())).keySet(),
            roleKeys
        )) {
            log(
                logPermission,
                Level.ERROR,
                "access denied: privilege={}, role={}, userKey={} due to role is missing",
                privilege,
                roleKeys,
                getUserKey()
            );
            throw new AccessDeniedException("Access is denied");
        }

        return false;
    }

    private boolean isConditionValid(Collection<Permission> permissions, StandardEvaluationContext context,
                                     Function<Permission, Expression> func) {
        return permissions.stream()
            .anyMatch(permission ->
                {
                    Expression expression = func.apply(permission);
                    if (isNull(expression) || isEmpty(expression.getExpressionString())) {
                        return true;
                    }
                    try {
                        return expression.getValue(context, Boolean.class);
                    } catch (Exception e) {
                        log.error("Exception while getting value ", e);
                        return false;
                    }
                }
            );
    }

    private boolean isPermissionEnabled(Collection<Permission> permissions) {
        return permissions.stream()
            .filter(Objects::nonNull)
            .anyMatch(permission -> !permission.isDisabled());
    }

    Map<String, Subject> getSubjects(Collection<String> roleKeys) {
        XmAuthenticationContext authContext = xmAuthenticationContextHolder.getContext();

        return roleKeys.stream()
            .collect(Collectors.toMap(
                roleKey -> roleKey,
                roleKey ->
                    new Subject(
                        authContext.getLogin().orElse(null),
                        authContext.getUserKey().orElse(null),
                        roleKey
                    )
                )
            );
    }

    private String getUserKey() {
        return xmAuthenticationContextHolder.getContext().getUserKey().orElse(null);
    }

    Collection<String> getRoleKeys(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(toList());
    }

    Collection<Permission> getPermissions(Collection<String> roleKeys, Object privilegeKey) {
        Map<String, Permission> permissions = permissionService
            .getPermissions(getRequiredTenantKeyValue(tenantContextHolder.getContext()));

        return roleKeys.stream()
            .map(roleKey -> permissions.get(roleKey + ":" + privilegeKey))
            .collect(toList());
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
}

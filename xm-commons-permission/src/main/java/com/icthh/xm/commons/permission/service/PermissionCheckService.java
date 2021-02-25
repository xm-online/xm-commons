package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.access.subject.Subject;
import com.icthh.xm.commons.permission.service.rolestrategy.RoleStrategy;
import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import com.icthh.xm.commons.permission.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;

@Slf4j
@Service
@IgnoreLogginAspect
public class PermissionCheckService {

    private static final String MULTI_ROLE_FIELD_NAME = "multiRoleEnabled";

    private final RoleStrategy multiRoleStrategy;
    private final RoleStrategy singleRoleStrategy;

    public PermissionCheckService(@Qualifier("multiRoleStrategy") final RoleStrategy multiRoleStrategy,
                                  @Qualifier("singleRoleStrategy") final RoleStrategy singleRoleStrategy) {
        this.multiRoleStrategy = multiRoleStrategy;
        this.singleRoleStrategy = singleRoleStrategy;
    }

    /**
     * Check permission for role and privilege key only.
     *
     * @param authentication the authentication
     * @param privilege the privilege key
     * @return true if permitted
     */
    public boolean hasPermission(Authentication authentication, Object privilege) {
        return withStrategy(authentication).hasPermission(authentication, privilege);
    }

    /**
     * Check permission for role, privilege key and resource condition.
     *
     * @param authentication the authentication
     * @param resource the resource
     * @param privilege the privilege key
     * @return true if permitted
     */
    public boolean hasPermission(Authentication authentication, Object resource, Object privilege) {
        return withStrategy(authentication).hasPermission(authentication, resource, privilege);
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
    public boolean hasPermission(Authentication authentication, Serializable resource, String resourceType,
                                 Object privilege) {
        return withStrategy(authentication).hasPermission(authentication, resource, resourceType, privilege);
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
    public Collection<String> createCondition(Authentication authentication, Object privilegeKey,
                                              SpelTranslator translator) {
        return withStrategy(authentication).createCondition(authentication, privilegeKey, translator);
    }

    private RoleStrategy withStrategy(Authentication authentication) {
        return isMultiRoleEnabled(authentication) ? multiRoleStrategy : singleRoleStrategy;
    }

    boolean isMultiRoleEnabled(final Authentication authentication) {
        try {
            return SecurityUtils.getAdditionalDetailsValueBoolean(authentication, MULTI_ROLE_FIELD_NAME);
        } catch (Exception e) {
            log.error("Multi-role check failed, set multi-role as false, error: {}", e.getMessage(), e);
            return false;
        }
    }
}

package com.icthh.xm.commons.permission.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.access.subject.Subject;
import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import com.icthh.xm.commons.permission.utils.SecurityUtils;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@IgnoreLogginAspect
public class PermissionCheckService {

    private static final String MULTIROLE_FIELD_NAME = "multiRoleEnabled";

    private final SecurityUtils securityUtils;
    private final MultiRolePermissionCheckService multiRolePermissionCheckService;
    private final SingleRolePermissionCheckService singleRolePermissionCheckService;

    /**
     * Check permission for role and privilege key only.
     *
     * @param authentication the authentication
     * @param privilege the privilege key
     * @return true if permitted
     */
    public boolean hasPermission(Authentication authentication, Object privilege) {

        try {
            Optional<Boolean> multiRoleEnabled = securityUtils.getAdditionalDetailsValueBoolean(authentication, MULTIROLE_FIELD_NAME);

            if (multiRoleEnabled.isPresent() && multiRoleEnabled.get()) {
                return multiRolePermissionCheckService.hasPermission(authentication, privilege);
            }
        } catch (Exception ex) {
            log.error("Multirole check exception hasPermission privilege", ex);
        }

        return singleRolePermissionCheckService.hasPermission(authentication, privilege);
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
        try {
            Optional<Boolean> multiRoleEnabled =
                securityUtils.getAdditionalDetailsValueBoolean(authentication, MULTIROLE_FIELD_NAME);

            if (multiRoleEnabled.isPresent() && multiRoleEnabled.get()) {
                return multiRolePermissionCheckService.hasPermission(authentication, resource, privilege);
            }
        } catch (Exception ex) {
            log.error("Multirole check exception hasPermission resource", ex);
        }

        return singleRolePermissionCheckService.hasPermission(authentication, resource, privilege);
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

        try {
            Optional<Boolean> multiRoleEnabled =
                securityUtils.getAdditionalDetailsValueBoolean(authentication, MULTIROLE_FIELD_NAME);

            if (multiRoleEnabled.isPresent() && multiRoleEnabled.get()) {
                return multiRolePermissionCheckService.hasPermission(authentication, resource, resourceType, privilege);
            }
        } catch (Exception ex) {
            log.error("Multirole check exception for hasPermission resourceType", ex);
        }

        return singleRolePermissionCheckService.hasPermission(authentication, resource, resourceType, privilege);
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
    public Collection<String> createCondition(
        Authentication authentication, Object privilegeKey, SpelTranslator translator
    ) {
        try {
            Optional<Boolean> multiRoleEnabled =
                securityUtils.getAdditionalDetailsValueBoolean(authentication, MULTIROLE_FIELD_NAME);

            if (multiRoleEnabled.isPresent() && multiRoleEnabled.get()) {
                return multiRolePermissionCheckService.createCondition(authentication, privilegeKey, translator);
            }
        } catch (Exception ex) {
            log.error("Multirole check exception on create condition", ex);
        }

        return ofNullable(singleRolePermissionCheckService.createCondition(authentication, privilegeKey, translator))
            .filter(StringUtils::isNotEmpty)
            .map(Collections::singletonList)
            .orElse(emptyList());
    }
}

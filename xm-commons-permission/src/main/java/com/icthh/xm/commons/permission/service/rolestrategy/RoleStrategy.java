package com.icthh.xm.commons.permission.service.rolestrategy;

import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import java.io.Serializable;
import java.util.Map;
import org.springframework.security.core.Authentication;

public interface RoleStrategy {

    boolean hasPermission(Authentication authentication, Object privilege);

    boolean hasPermission(Authentication authentication, Object resource, Object privilege);

    boolean hasPermission(Authentication authentication, Serializable resource, String resourceType, Object privilege);

    default String createCondition(Authentication authentication, Object privilegeKey, SpelTranslator translator) {
        return createCondition(authentication, privilegeKey, translator, Map.of());
    }

    String createCondition(Authentication authentication, Object privilegeKey, SpelTranslator translator,
                           Map<String, Object> contextParams);
}

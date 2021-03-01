package com.icthh.xm.commons.permission.service.rolestrategy;

import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import java.io.Serializable;
import java.util.Collection;
import org.springframework.security.core.Authentication;

public interface RoleStrategy {

    boolean hasPermission(Authentication authentication, Object privilege);

    boolean hasPermission(Authentication authentication, Object resource, Object privilege);

    boolean hasPermission(Authentication authentication, Serializable resource, String resourceType, Object privilege);

    Collection<String> createCondition(Authentication authentication, Object privilegeKey, SpelTranslator translator);
}

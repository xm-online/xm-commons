package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.keyresolver.LepSegmentKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

@Slf4j
@LepService(group = "service.permission")
public class PermissionLepService {

    @LogicExtensionPoint(value = "Check", resolver = LepSegmentKeyResolver.class)
    public boolean check(String lepSegment, String privilege, String roleKey, Map<String, Object> params,
                         Map<String, Object> contextParams) {
        log.warn("Permission check LEP is not defined, access denied: lepSegment={}, privilege={}, role={}",
            lepSegment, privilege, roleKey);
        return false;
    }

    @LogicExtensionPoint(value = "Filter", resolver = LepSegmentKeyResolver.class)
    public String filter(String lepSegment, String privilege, String roleKey, Map<String, Object> params,
                         Map<String, Object> contextParams) {
        log.warn("Permission filter LEP is not defined, access denied: lepSegment={}, privilege={}, role={}",
            lepSegment, privilege, roleKey);
        throw new AccessDeniedException("Access is denied");
    }
}

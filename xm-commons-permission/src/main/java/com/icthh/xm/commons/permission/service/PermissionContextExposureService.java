package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.domain.dto.PermissionContextDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LepService(group = "service.context.expose")
public class PermissionContextExposureService {

    @LogicExtensionPoint("ExposePermissionContext")
    public PermissionContextDto exposePermissionContext(String userKey) {
        log.info("Expose empty auth permission context by userKey: {}", userKey);
        return new PermissionContextDto();
    }
}

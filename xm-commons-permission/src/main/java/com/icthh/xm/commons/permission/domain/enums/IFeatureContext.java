package com.icthh.xm.commons.permission.domain.enums;

import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;

public interface IFeatureContext {

    /**
     * This method checks weather feature is enabled in tenant config.
     * @param service - DynamicPermissionCheckService instance
     * @return true if enabled
     */
    boolean isEnabled(DynamicPermissionCheckService service);
}

package com.icthh.xm.commons.permission.service;

import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;

public interface DynamicPermissionCheckService {

    boolean checkContextPermission(IFeatureContext featureContext, String basePermission, String suffix);

}

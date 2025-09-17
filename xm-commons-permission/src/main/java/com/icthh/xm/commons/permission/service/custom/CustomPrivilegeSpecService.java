package com.icthh.xm.commons.permission.service.custom;

import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;

import java.util.Collection;

public interface CustomPrivilegeSpecService {

    <S extends ConfigWithKey> void onSpecificationUpdate(Collection<S> specs, String tenantKey);
}

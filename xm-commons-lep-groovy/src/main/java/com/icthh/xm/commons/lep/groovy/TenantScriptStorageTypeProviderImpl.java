package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TenantScriptStorageTypeProviderImpl implements TenantScriptStorageTypeProvider {
    @Getter
    private final TenantScriptStorage tenantScriptStorage;
}

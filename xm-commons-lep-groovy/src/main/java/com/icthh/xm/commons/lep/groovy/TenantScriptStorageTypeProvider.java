package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.TenantScriptStorage;

@FunctionalInterface
public interface TenantScriptStorageTypeProvider {
    TenantScriptStorage getTenantScriptStorage();
}

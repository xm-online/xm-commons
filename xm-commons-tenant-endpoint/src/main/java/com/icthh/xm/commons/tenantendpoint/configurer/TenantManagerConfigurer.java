package com.icthh.xm.commons.tenantendpoint.configurer;

import com.icthh.xm.commons.tenantendpoint.TenantManager;

public interface TenantManagerConfigurer {
    void configure(TenantManager.TenantManagerBuilder builder);
}

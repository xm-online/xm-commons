package com.icthh.xm.commons.domain.event.util;

import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TenantUtil {

    public static String normalizeTenant(String tenant) {
        if (isTenantKeyValid(tenant)) {
            return tenant;
        } else {
            String normalizedTenant = tenant.replace("-", "_");
            if (isTenantKeyValid(normalizedTenant)) {
                return normalizedTenant;
            } else {
                throw new IllegalArgumentException(String.format("Tenant %s cannot be normalized.", tenant));
            }
        }
    }

    public static boolean isTenantKeyValid(String tenantKey) {
        if (tenantKey == null) {
            return false;
        }
        return TenantContextUtils.isTenantKeyValid(tenantKey);
    }
}

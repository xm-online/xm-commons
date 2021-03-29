package com.icthh.xm.commons.permission.service.filter;

@FunctionalInterface
public interface PermissionMsNameFilter {
    boolean filterPermission(String permissionMsName);
}

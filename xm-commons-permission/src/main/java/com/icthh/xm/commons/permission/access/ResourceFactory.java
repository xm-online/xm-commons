package com.icthh.xm.commons.permission.access;

public interface ResourceFactory {

    <T, ID> T getResource(ID resourceId, String objectType);
}

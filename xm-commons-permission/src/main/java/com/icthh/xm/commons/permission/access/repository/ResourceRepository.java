package com.icthh.xm.commons.permission.access.repository;

public interface ResourceRepository<T, ID> {
    T findResourceById(ID id);
}

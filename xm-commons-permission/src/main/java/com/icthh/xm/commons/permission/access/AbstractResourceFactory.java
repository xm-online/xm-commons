package com.icthh.xm.commons.permission.access;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;

import java.util.Map;

public abstract class AbstractResourceFactory implements ResourceFactory {

        protected abstract Map<String, ? extends ResourceRepository> getRepositories();

        @Override
        public Object getResource(Object resourceId, String objectType) {
            Object result = null;
            ResourceRepository resourceRepository = getRepositories().get(objectType);
            if (resourceRepository != null) {
                result = resourceRepository.findResourceById(resourceId);
            }
            return result;
        }
}

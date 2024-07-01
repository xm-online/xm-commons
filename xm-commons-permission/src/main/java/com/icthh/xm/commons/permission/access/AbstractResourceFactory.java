package com.icthh.xm.commons.permission.access;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;

import java.util.Map;

public abstract class AbstractResourceFactory implements ResourceFactory {

        protected abstract Map<String, ? extends ResourceRepository<?, ?>> getRepositories();

        @Override
        public <T, ID> T getResource(ID resourceId, String objectType) {
            T result = null;
            ResourceRepository<T, ID> resourceRepository = (ResourceRepository<T, ID>) getRepositories().get(objectType);
            if (resourceRepository != null) {
                result = resourceRepository.findResourceById(resourceId);
            }
            return result;
        }
}

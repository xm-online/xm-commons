package com.icthh.xm.commons.lep;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Objects;

/**
 * The {@link RouterResourceLoader} class.
 */
public class RouterResourceLoader implements ResourceLoader {

    private final Map<String, ResourceLoader> urlPrefixToResourceLoader;

    public RouterResourceLoader(Map<String, ResourceLoader> urlPrefixToResourceLoader) {
        this.urlPrefixToResourceLoader = Objects.requireNonNull(urlPrefixToResourceLoader);
    }

    public ResourceLoader getResourceLoader(String prefix) {
        return urlPrefixToResourceLoader.get(prefix);
    }

    private static String getUrlPrefix(String location) {
        if (StringUtils.isBlank(location)) {
            return null;
        }

        int prefixEndIndex = location.indexOf(':');
        if (prefixEndIndex <= 0) {
            return null;
        }

        return location.substring(0, prefixEndIndex + 1);
    }

    @Override
    public Resource getResource(String location) {
        String urlPrefix = getUrlPrefix(location);
        if (urlPrefix == null) {
            throw new IllegalStateException("Can't detect URL prefix for location: " + location);
        }

        ResourceLoader resourceLoader = urlPrefixToResourceLoader.get(urlPrefix);
        if (resourceLoader == null) {
            throw new IllegalStateException("Unsupported resource URL prefix: " + urlPrefix);
        }

        return resourceLoader.getResource(location);
    }

    @Override
    public ClassLoader getClassLoader() {
        ResourceLoader resourceLoader = urlPrefixToResourceLoader.get(ResourceLoader.CLASSPATH_URL_PREFIX);
        if (resourceLoader == null) {
            resourceLoader = urlPrefixToResourceLoader.values().stream().findFirst().orElse(null);
        }

        if (resourceLoader == null) {
            return ClassUtils.getDefaultClassLoader();
        }

        return resourceLoader.getClassLoader();
    }

}

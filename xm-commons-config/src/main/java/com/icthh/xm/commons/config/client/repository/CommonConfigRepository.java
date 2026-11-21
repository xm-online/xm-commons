package com.icthh.xm.commons.config.client.repository;

import com.icthh.xm.commons.config.domain.Configuration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface CommonConfigRepository {

    Map<String, Configuration> getConfig(String commit);

    Map<String,Configuration> getConfig(String version, Collection<String> paths);
    Map<String,Configuration> getConfigByPatternPaths(String version, Collection<String> paths);

    void updateConfigFullPath(Configuration configuration, String oldConfigHash);

    /**
     * Current ms-config version (the JSON of ConfigVersion or a plain string).
     * Empty when the repository has no versions or the version endpoint is not available.
     * Any other failure throws.
     */
    default Optional<String> getVersion() {
        return Optional.empty();
    }

}

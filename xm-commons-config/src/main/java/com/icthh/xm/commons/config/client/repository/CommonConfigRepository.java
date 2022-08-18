package com.icthh.xm.commons.config.client.repository;

import com.icthh.xm.commons.config.domain.Configuration;
import java.util.Collection;
import java.util.Map;

public interface CommonConfigRepository {

    Map<String, Configuration> getConfig(String commit);

    Map<String,Configuration> getConfig(String version, Collection<String> paths);

    void updateConfigFullPath(Configuration configuration, String oldConfigHash);

}

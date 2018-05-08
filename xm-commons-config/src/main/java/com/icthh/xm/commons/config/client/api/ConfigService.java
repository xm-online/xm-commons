package com.icthh.xm.commons.config.client.api;

import com.icthh.xm.commons.config.domain.Configuration;

import java.util.Map;

public interface ConfigService {

    Map<String, Configuration> getConfig();
}

package com.icthh.xm.commons.config.client.service;

import com.icthh.xm.commons.config.client.api.AbstractConfigService;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CommonConfigService extends AbstractConfigService {

    private final CommonConfigRepository commonConfigRepository;

    @Override
    public Map<String, Configuration> getConfigurationMap(String commit) {
        return commonConfigRepository.getConfig(commit);
    }

    @Override
    public Map<String, Configuration> getConfigurationMap(String commit, Collection<String> paths) {
        return commonConfigRepository.getConfig(commit, paths);
    }
}

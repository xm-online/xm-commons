package com.icthh.xm.commons.permission.service.custom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.icthh.xm.commons.config.client.api.refreshable.ConfigWithKey;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.config.client.repository.TenantConfigRepository.PATH_CONFIG_TENANT;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCustomPrivilegeSpecService implements CustomPrivilegeSpecService {

    private static final String TENANT_NAME = "tenantName";
    private static final String CUSTOMER_PRIVILEGES_PATH = PATH_CONFIG_TENANT + "custom-privileges.yml";

    private final YAMLFactory yamlFactory = new YAMLFactory().enable(YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS);

    private final ObjectMapper mapper = new ObjectMapper(yamlFactory);

    private final CommonConfigRepository commonConfigRepository;

    private final List<CustomPrivilegesExtractor> privilegesExtractors;

    @Override
    public <S extends ConfigWithKey> void onSpecificationUpdate(Collection<S> specs, String tenantKey) {
        String privilegesPath = resolvePathWithTenant(tenantKey);
        Configuration customPrivilegesConfig = getConfigByPath(privilegesPath);
        updateCustomPrivileges(specs, privilegesPath, customPrivilegesConfig, tenantKey);
    }

    protected  Configuration getConfigByPath(String privilegesPath) {
        log.info("Get custom-privileges actual config by path {}", privilegesPath);

        List<String> paths = List.of(privilegesPath);
        Map<String, Configuration> configs = commonConfigRepository.getConfig(null, paths);
        configs = configs == null ? new HashMap<>() : configs;

        return configs.get(privilegesPath);
    }

    private String resolvePathWithTenant(String tenantKey) {
        return CUSTOMER_PRIVILEGES_PATH.replace("{" + TENANT_NAME + "}", tenantKey);
    }

    @SneakyThrows
    protected <S extends ConfigWithKey> void updateCustomPrivileges(Collection<S> specs, String privilegesPath,
                                                                    Configuration customPrivileges, String tenantKey) {
        val privileges = readPrivilegesConfig(customPrivileges);

        addCustomPrivileges(specs, privileges, tenantKey);

        String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(privileges);
        if (DigestUtils.sha1Hex(content).equals(sha1Hex(customPrivileges))) {
            log.info("Privileges configuration not changed y path: {}", privilegesPath);
            return;
        }
        commonConfigRepository.updateConfigFullPath(new Configuration(privilegesPath, content), sha1Hex(customPrivileges));
    }

    private <S extends ConfigWithKey> void addCustomPrivileges(Collection<S> specs,
                                     Map<String, List<Map<String, Object>>> privileges,
                                     String tenantKey) {
        Map<String, List<Map<String, Object>>> customPrivileges = new HashMap<>();

        privilegesExtractors.stream()
            .filter(extractor -> extractor.isEnabled(tenantKey))
            .forEach(extractor -> {
                var section = customPrivileges.computeIfAbsent(extractor.getSectionName(), k -> new ArrayList<>());
                section.addAll(extractor.toPrivileges(specs));
                section.sort(comparing(it -> String.valueOf(it.get("key"))));
            });

        privileges.putAll(customPrivileges);
    }

    @SneakyThrows
    private Map<String, List<Map<String, Object>>> readPrivilegesConfig(Configuration customPrivileges) {
        if (isConfigExists(customPrivileges)) {
            return mapper.readValue(customPrivileges.getContent(),
                new TypeReference<Map<String, List<Map<String, Object>>>>() {}
            );
        }
        return new HashMap<>();
    }

    private String sha1Hex(Configuration configuration) {
        return ofNullable(configuration).map(Configuration::getContent).map(DigestUtils::sha1Hex).orElse(null);
    }

    private boolean isConfigExists(Configuration configuration) {
        return ofNullable(configuration)
            .map(Configuration::getContent)
            .map(StringUtils::isNotBlank)
            .orElse(false);
    }
}

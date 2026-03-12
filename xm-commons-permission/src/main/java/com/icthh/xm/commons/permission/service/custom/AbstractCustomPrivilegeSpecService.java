package com.icthh.xm.commons.permission.service.custom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import java.util.LinkedHashMap;

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

    protected Configuration getConfigByPath(String privilegesPath) {
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
        val updatedCustomPrivileges = applyCustomPrivileges(specs, privileges, tenantKey);

        ObjectWriter prettyPrinter = mapper.writerWithDefaultPrettyPrinter();
        String content = prettyPrinter.writeValueAsString(privileges);
        String updatedContent = prettyPrinter.writeValueAsString(updatedCustomPrivileges);

        if (DigestUtils.sha1Hex(content).equals(DigestUtils.sha1Hex(updatedContent))) {
            log.info("Privileges configuration not changed y path: {}", privilegesPath);
            return;
        }
        commonConfigRepository.updateConfigFullPath(new Configuration(privilegesPath, updatedContent), sha1Hex(customPrivileges));
    }

    private <S extends ConfigWithKey> Map<String, List<Map<String, Object>>> applyCustomPrivileges(Collection<S> specs,
                                                                                                   Map<String, List<Map<String, Object>>> privileges,
                                                                                                   String tenantKey) {
        Map<String, List<Map<String, Object>>> customPrivileges = new HashMap<>();
        Map<String, List<Map<String, Object>>> updatedCustomPrivileges = new LinkedHashMap<>(privileges);

        privilegesExtractors.stream()
            .filter(extractor -> extractor.isEnabled(tenantKey))
            .forEach(extractor -> {
                var section = customPrivileges.computeIfAbsent(extractor.getSectionName(), k -> new ArrayList<>());
                section.addAll(extractor.toPrivileges(specs));
                section.sort(comparing(it -> String.valueOf(it.get("key"))));
            });

        updatedCustomPrivileges.putAll(customPrivileges);

        customPrivileges.keySet().forEach(section ->
            ofNullable(privileges.get(section))
                .ifPresent(list -> list.sort(comparing(it -> String.valueOf(it.get("key")))))
        );

        return updatedCustomPrivileges;
    }

    @SneakyThrows
    private Map<String, List<Map<String, Object>>> readPrivilegesConfig(Configuration customPrivileges) {
        if (isConfigExists(customPrivileges)) {
            return mapper.readValue(customPrivileges.getContent(),
                new TypeReference<Map<String, List<Map<String, Object>>>>() {});
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

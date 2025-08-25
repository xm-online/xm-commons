package com.icthh.xm.commons.service.impl;

import static com.icthh.xm.commons.utils.YamlPatchUtils.addSequenceItem;
import static com.icthh.xm.commons.utils.YamlPatchUtils.arrayByField;
import static com.icthh.xm.commons.utils.YamlPatchUtils.delete;
import static com.icthh.xm.commons.utils.YamlPatchUtils.key;
import static com.icthh.xm.commons.utils.YamlPatchUtils.updateSequenceItem;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.service.FunctionManageService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.utils.YamlPatchUtils.YamlPatchPattern;
import io.micrometer.common.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FunctionManageServiceImpl implements FunctionManageService<FunctionSpec, FunctionSpecWithFileName<FunctionSpec>> {

    private final FunctionApiSpecConfiguration specService;
    private final TenantContextHolder tenantContextHolder;
    private final CommonConfigRepository commonConfigRepository;
    private final CommonConfigService commonConfigService;

    @Override
    public void addFunction(FunctionSpecWithFileName<FunctionSpec> newFunction) {
        assertKeyUnique(newFunction);

        Configuration configuration = findOriginalConfig(newFunction.getFileKey());
        String yamlFile = configuration.getContent();
        if (StringUtils.isBlank(yamlFile)) {
            yamlFile = "functions:\n";
        }
        FunctionSpec item = newFunction.getItem();
        String updatedYaml = addSequenceItem(yamlFile, item, addPath());
        Configuration updatedConfig = new Configuration(configuration.getPath(), updatedYaml);
        commonConfigRepository.updateConfigFullPath(updatedConfig, null);
        commonConfigService.notifyUpdated(updatedConfig);
    }

    protected List<YamlPatchPattern> addPath() {
        return List.of(key("functions"));
    }

    protected List<YamlPatchPattern> removePath(String functionKey) {
        return List.of(key("functions"), arrayByField(Map.of("key", functionKey)));
    }

    private void assertKeyUnique(FunctionSpecWithFileName<FunctionSpec> newFunction) {
        specService.getSpecByKeyAndTenant(newFunction.getItem().getKey(), tenantContextHolder.getTenantKey())
            .ifPresent(spec -> {
                log.error("Function with key '{}' already exists", newFunction.getItem().getKey());
                throw new BusinessException("error.function.with.key.already.exists", "Function already exists");
            });
    }

    private void assertKeyExists(FunctionSpecWithFileName<FunctionSpec> updatedFunction) {
        specService.getSpecByKeyAndTenant(updatedFunction.getItem().getKey(), tenantContextHolder.getTenantKey())
            .orElseThrow(() -> throwFunctionNotFound(updatedFunction.getItem().getKey()));
    }

    private static BusinessException throwFunctionNotFound(String functionKey) {
        log.error("Function with key '{}' not exists", functionKey);
        return new BusinessException("error.function.with.key.not.exists", "Function not exists");
    }

    @Override
    public void updateFunction(FunctionSpecWithFileName<FunctionSpec> updatedFunction) {
        assertKeyExists(updatedFunction);
        String functionKey = updatedFunction.getItem().getKey();
        String oldFileKey = getFileKeyByFunctionKey(functionKey);
        if (Objects.equals(oldFileKey, updatedFunction.getFileKey())) {
            Configuration originalConfig = findOriginalConfig(oldFileKey);
            String updatedYaml = updateSequenceItem(originalConfig.getContent(), updatedFunction.getItem(), removePath(functionKey));
            Configuration updatedConfig = new Configuration(originalConfig.getPath(), updatedYaml);
            commonConfigRepository.updateConfigFullPath(updatedConfig, null);
            commonConfigService.notifyUpdated(updatedConfig);
        } else {
            removeFunction(functionKey);
            addFunction(updatedFunction);
        }
    }

    @Override
    public void removeFunction(String functionKey) {
        String fileKey = getFileKeyByFunctionKey(functionKey);
        Configuration configuration = findOriginalConfig(fileKey);
        if (configuration.getContent() != null) {
            String updatedYaml = delete(configuration.getContent(), removePath(functionKey));
            Configuration updatedConfig = new Configuration(configuration.getPath(), updatedYaml);
            commonConfigRepository.updateConfigFullPath(updatedConfig, null);
            commonConfigService.notifyUpdated(updatedConfig);
        } else {
            throw new EntityNotFoundException("Function not found, fileKey: " + fileKey + ", functionKey: " + functionKey);
        }
    }

    @Override
    public TypeReference<FunctionSpecWithFileName<FunctionSpec>> getFunctionSpecWrapperClass() {
        return new TypeReference<>() {};
    }

    @Override
    public TypeReference<FunctionSpec> getFunctionSpecClass() {
        return new TypeReference<>() {};
    }

    protected Configuration findOriginalConfig(String fileKey) {
        String tenantKey = tenantContextHolder.getTenantKey();
        Map<String, String> specFiles = specService.getSpecFiles(tenantKey);
        String filePath = buildFilePath(fileKey, tenantKey);
        String yamlFile = specFiles.get(filePath);
        return new Configuration(filePath, yamlFile);
    }

    protected String buildFilePath(String fileKey, String tenantKey) {
        String folder = specService.getSpecFolder(tenantKey);
        return fileKey == null ? folder + ".yml" : folder + "/" + fileKey + ".yml";
    }

    protected String getFileKeyByFunctionKey(String functionKey) {
        String tenantKey = tenantContextHolder.getTenantKey();
        var spec = specService.getFunctionSpecsWithFileName(tenantKey).stream()
            .filter(e -> nonNull(e.getItem()))
            .filter(e -> Objects.equals(e.getItem().getKey(), functionKey))
            .findFirst().orElseThrow(() -> throwFunctionNotFound(functionKey));
        return spec.getFileKey();
    }

}

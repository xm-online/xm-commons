package com.icthh.xm.commons.config;

import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.domain.comparator.FunctionSpecPathComparator;
import com.icthh.xm.commons.domain.spec.FunctionApiSpecs;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import java.util.List;
import java.util.Map;

import com.icthh.xm.commons.permission.service.custom.CustomPrivilegeSpecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.enums.SpecPathPatternEnum.findTenantName;
import static com.icthh.xm.commons.utils.Constants.FUNCTIONS;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Slf4j
@Service
@IgnoreLogginAspect
public class FunctionApiSpecConfiguration extends DataSpecificationService<FunctionApiSpecs> {

    private final String appName;
    private final CustomPrivilegeSpecService customPrivilegeSpecService;

    public FunctionApiSpecConfiguration(@Value("${spring.application.name}") String appName,
                                        JsonListenerService jsonListenerService,
                                        CustomPrivilegeSpecService customPrivilegeSpecService,
                                        FunctionApiSpecsProcessor functionApiSpecsProcessor) {
        super(FunctionApiSpecs.class, jsonListenerService, functionApiSpecsProcessor);
        this.appName = appName;
        this.customPrivilegeSpecService = customPrivilegeSpecService;
    }

    @Override
    public String specKey() {
        return FUNCTIONS;
    }

    @Override
    public String folder() {
        return appName + "/" + FUNCTIONS;
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        super.refreshFinished(paths);

        Set<String> tenants = paths.stream()
            .map(p -> findTenantName(p, folder()))
            .collect(Collectors.toSet());

        tenants.forEach(tenantKey -> {
            Collection<FunctionSpec> specByTenant = getOrderedSpecByTenant(tenantKey);
            customPrivilegeSpecService.onSpecificationUpdate(specByTenant, tenantKey);
        });
    }

    public Optional<FunctionSpec> getSpecByKeyAndTenant(String functionKey, String tenantKey) {
        return getTenantSpecifications(tenantKey).values().stream()
            .map(FunctionApiSpecs::getItems)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(f -> functionKey.equals(f.getKey()))
            .findFirst();
    }

    public Collection<FunctionSpec> getOrderedSpecByTenant(String tenantKey) {
        return getTenantSpecifications(tenantKey).values().stream()
            .map(FunctionApiSpecs::getItems)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .sorted(FunctionSpecPathComparator.of())
            .toList();
    }

    public Collection<FunctionSpecWithFileName<FunctionSpec>> getFunctionSpecsWithFileName(String tenantKey) {
        Map<String, FunctionApiSpecs> specificationsMap = getSpecificationsMapFromFiles(tenantKey);
        return specificationsMap.entrySet().stream()
            .filter(e -> nonNull(e.getValue()))
            .filter(e -> nonNull(e.getValue().getItems()))
            .flatMap(e ->
                e.getValue().getItems()
                    .stream()
                    .map(f -> new FunctionSpecWithFileName<>(f, extractFileName(tenantKey, e)))
            ).toList();
    }

    private String extractFileName(String tenantKey, Map.Entry<String, FunctionApiSpecs> e) {
        String prefix = getSpecFolder(tenantKey) + "/";
        if (nonNull(e.getKey()) && e.getKey().endsWith(".yml") && e.getKey().startsWith(prefix)) {
            return e.getKey().substring(prefix.length(), e.getKey().lastIndexOf('.'));
        }
        return null;
    }

    public String getSpecFolder(String tenantKey) {
        return "/config/tenants/" + tenantKey + "/" + folder();
    }

    @Override
    protected Map<String, FunctionApiSpecs> getSpecificationsMapFromFiles(String tenantKey) {
        Map<String, FunctionApiSpecs> fileToSpec = super.getSpecificationsMapFromFiles(tenantKey);

        fileToSpec.entrySet().stream()
            .filter(e -> nonNull(e.getValue()))
            .filter(e -> nonNull(e.getValue().getItems()))
            .filter(e -> nonNull(extractFileName(tenantKey, e)))
            .forEach(e -> {
                String fileKey = extractFileName(tenantKey, e);
                e.getValue().getItems()
                    .stream()
                    .filter(item -> isEmpty(item.getTags()))
                    .forEach(item -> item.setTags(List.of(fileKey)));
            });

        return fileToSpec;
    }
}


package com.icthh.xm.commons.config;

import com.icthh.xm.commons.domain.comparator.FunctionSpecPathComparator;
import com.icthh.xm.commons.domain.spec.FunctionApiSpecs;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static com.icthh.xm.commons.utils.Constants.FUNCTIONS;

@Slf4j
@Service
@IgnoreLogginAspect
public class FunctionApiSpecConfiguration extends DataSpecificationService<FunctionApiSpecs> {

    private final String appName;

    public FunctionApiSpecConfiguration(@Value("${spring.application.name}") String appName,
                                        JsonListenerService jsonListenerService,
                                        FunctionApiSpecsProcessor functionApiSpecsProcessor) {
        super(FunctionApiSpecs.class, jsonListenerService, functionApiSpecsProcessor);
        this.appName = appName;
    }

    @Override
    public String specKey() {
        return FUNCTIONS;
    }

    @Override
    public String folder() {
        return appName + "/" + FUNCTIONS;
    }

    public Optional<FunctionSpec> getSpecByKeyAndTenant(String functionKey, String tenantKey) {
        return getTenantSpecifications(tenantKey).values().stream()
            .map(FunctionApiSpecs::getFunctions)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(f -> functionKey.equals(f.getKey()))
            .findFirst();
    }

    public Collection<FunctionSpec> getOrderedSpecByTenant(String tenantKey) {
        return getTenantSpecifications(tenantKey).values().stream()
            .map(FunctionApiSpecs::getFunctions)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .sorted(FunctionSpecPathComparator.of())
            .toList();
    }
}


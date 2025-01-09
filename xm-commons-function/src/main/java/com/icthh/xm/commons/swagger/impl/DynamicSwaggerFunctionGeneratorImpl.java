package com.icthh.xm.commons.swagger.impl;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerRefreshableConfiguration;
import com.icthh.xm.commons.domain.spec.FunctionApiSpecs;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.icthh.xm.commons.utils.CollectionsUtils.nullSafe;
import static com.icthh.xm.commons.utils.SwaggerGeneratorUtils.getSupportedHttpMethodFilters;
import static com.icthh.xm.commons.utils.FunctionSpecUtils.byFilters;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Component("dynamicSwaggerFunctionGenerator")
public class DynamicSwaggerFunctionGeneratorImpl extends DefaultDynamicSwaggerFunctionGenerator<FunctionSpec> {

    public static final List<String> DEFAULT_METHODS = List.of(GET.name(), POST.name());
    private final FunctionApiSpecConfiguration functionApiSpecConfiguration;
    private final TenantContextHolder tenantContextHolder;

    public DynamicSwaggerFunctionGeneratorImpl(@Value("${spring.application.name}") String appName,
                                               DynamicSwaggerRefreshableConfiguration dynamicSwaggerService,
                                               FunctionApiSpecConfiguration functionApiSpecConfiguration,
                                               TenantContextHolder tenantContextHolder) {
        super(appName, dynamicSwaggerService);
        this.functionApiSpecConfiguration = functionApiSpecConfiguration;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public List<FunctionSpec> getFunctionSpecs(DynamicSwaggerConfiguration swaggerConfig) {
        Collection<FunctionApiSpecs> specs = getAllTenantSpecifications();
        return specs.stream()
            .map(FunctionApiSpecs::getItems)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .filter(byFilters(swaggerConfig))
            .collect(toList());
    }

    @Override
    public List<String> getFunctionHttpMethods(FunctionSpec functionSpec) {
        List<String> httpMethods = isEmpty(functionSpec.getHttpMethods()) ? DEFAULT_METHODS : functionSpec.getHttpMethods();

        getSupportedHttpMethodFilters().stream()
            .filter(f -> f.supported(functionSpec))
            .forEach(f -> f.filter(httpMethods));

        functionSpec.setHttpMethods(httpMethods);
        return httpMethods;
    }

    @Override
    public List<String> getFunctionTags(FunctionSpec functionSpec) {
        return nullSafe(functionSpec.getTags());
    }

    @Override
    public String getFunctionName(FunctionSpec functionSpec) {
        Map<String, String> nameMap = nullSafe(functionSpec.getName());
        return Optional.ofNullable(nameMap.get("en"))
            .orElse(nameMap.values().stream().findFirst().orElse(functionSpec.getKey()));
    }

    @Override
    public String getFunctionInputJsonSchema(FunctionSpec functionSpec) {
        return functionSpec.getInputDataSpec();
    }

    @Override
    public String getFunctionOutputJsonSchema(FunctionSpec functionSpec) {
        return functionSpec.getOutputDataSpec();
    }

    private Collection<FunctionApiSpecs> getAllTenantSpecifications() {
        String tenantName = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        return functionApiSpecConfiguration.getTenantSpecifications(tenantName).values();
    }
}

package com.icthh.xm.commons.swagger.impl;

import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerRefreshableConfiguration;
import com.icthh.xm.commons.domain.spec.IFunctionSpec;
import com.icthh.xm.commons.swagger.DynamicSwaggerFunctionGenerator;
import com.icthh.xm.commons.swagger.SwaggerGenerator;
import com.icthh.xm.commons.swagger.model.SwaggerFunction;
import com.icthh.xm.commons.swagger.model.SwaggerModel;
import com.icthh.xm.commons.swagger.model.SwaggerParameter;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.utils.SwaggerGeneratorUtils.makeAsPath;

public abstract class AbstractDynamicSwaggerFunctionGenerator<FS extends IFunctionSpec>
    implements DynamicSwaggerFunctionGenerator {

    private final DynamicSwaggerRefreshableConfiguration dynamicSwaggerService;

    public AbstractDynamicSwaggerFunctionGenerator(DynamicSwaggerRefreshableConfiguration dynamicSwaggerService) {
        this.dynamicSwaggerService = dynamicSwaggerService;
    }

    public DynamicSwaggerConfiguration getSwaggerConfiguration() {
        return dynamicSwaggerService.getConfiguration();
    }

    @Override
    public SwaggerModel generateSwagger(String baseUrl) {
        DynamicSwaggerConfiguration swaggerConfig = getSwaggerConfiguration();
        return generateSwagger(baseUrl, getFunctionSpecs(swaggerConfig));
    }

    public SwaggerModel generateSwagger(String baseUrl, List<FS> functionSpecs) {
        SwaggerGenerator swaggerGenerator = getSwaggerGenerator(baseUrl);
        functionSpecs.forEach(it -> generateSwaggerFunction(it, swaggerGenerator));
        SwaggerModel swaggerBody = swaggerGenerator.getSwaggerBody();
        enrichSwaggerBody(swaggerBody);
        return swaggerBody;
    }

    private void generateSwaggerFunction(FS functionSpec, SwaggerGenerator defaultSwaggerGenerator) {
        String prefix = buildPathPrefix(functionSpec);
        Map<String, SwaggerParameter> pathPrefixParams = buildPrefixPathParams(functionSpec);
        defaultSwaggerGenerator.generateFunction(prefix, pathPrefixParams, buildSwaggerFunction(functionSpec));
    }

    private SwaggerFunction buildSwaggerFunction(FS functionSpec) {
        SwaggerFunction swaggerFunction = new SwaggerFunction(
            functionSpec.getKey(),
            makeAsPath(functionSpec.getPath(), functionSpec.getKey()),
            getFunctionName(functionSpec),
            getFunctionDescription(functionSpec),
            getFunctionInputJsonSchema(functionSpec),
            getFunctionOutputJsonSchema(functionSpec),
            getFunctionTags(functionSpec),
            getFunctionHttpMethods(functionSpec),
            functionSpec.getWrapResult(),
            functionSpec.getAnonymous()
        );
        enrichSwaggerFunction(functionSpec, swaggerFunction);
        return swaggerFunction;
    }

    public abstract void enrichSwaggerBody(SwaggerModel swaggerBody);
    public abstract void enrichSwaggerFunction(FS it, SwaggerFunction swaggerFunction);

    public abstract SwaggerGenerator getSwaggerGenerator(String baseUrl);
    public abstract List<FS> getFunctionSpecs(DynamicSwaggerConfiguration swaggerConfig);
    @NotNull
    public abstract String buildPathPrefix(FS functionSpec);
    @NotNull
    public abstract Map<String, SwaggerParameter> buildPrefixPathParams(FS functionSpec);
    @NotNull
    public abstract List<String> getFunctionHttpMethods(FS functionSpec);
    @NotNull
    public abstract List<String> getFunctionTags(FS functionSpec);
    public abstract String getFunctionName(FS functionSpec);
    public abstract String getFunctionDescription(FS functionSpec);
    public abstract String getFunctionInputJsonSchema(FS functionSpec);
    public abstract String getFunctionOutputJsonSchema(FS functionSpec);
}

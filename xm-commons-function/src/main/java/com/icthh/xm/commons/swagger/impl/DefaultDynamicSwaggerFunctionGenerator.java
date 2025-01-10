package com.icthh.xm.commons.swagger.impl;

import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerRefreshableConfiguration;
import com.icthh.xm.commons.domain.spec.IFunctionSpec;
import com.icthh.xm.commons.swagger.SwaggerGenerator;
import com.icthh.xm.commons.swagger.model.SwaggerFunction;
import com.icthh.xm.commons.swagger.model.SwaggerModel;
import com.icthh.xm.commons.swagger.model.SwaggerParameter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public abstract class DefaultDynamicSwaggerFunctionGenerator<FS extends IFunctionSpec>
    extends AbstractDynamicSwaggerFunctionGenerator<FS> {

    private String appName;

    public DefaultDynamicSwaggerFunctionGenerator(@Value("${spring.application.name}") String appName,
                                                  DynamicSwaggerRefreshableConfiguration dynamicSwaggerService) {
        super(dynamicSwaggerService);
        this.appName = appName;
    }

    @Override
    public void enrichSwaggerBody(SwaggerModel swaggerBody) {

    }

    @Override
    public void enrichSwaggerFunction(FS it, SwaggerFunction swaggerFunction) {

    }

    @Override
    public SwaggerGenerator getSwaggerGenerator(String baseUrl) {
        DynamicSwaggerConfiguration swaggerConfig = getSwaggerConfiguration();
        return getSwaggerGenerator(baseUrl, swaggerConfig);
    }

    public SwaggerGenerator getSwaggerGenerator(String baseUrl, DynamicSwaggerConfiguration swaggerConfig) {
        return new DefaultSwaggerGenerator(baseUrl, swaggerConfig);
    }

    @Override
    public String buildPathPrefix(FS functionSpec) {
        return "/" + appName + "/api/functions" + (TRUE.equals(functionSpec.getAnonymous()) ? "/anonymous" : "");
    }

    @Override
    public Map<String, SwaggerParameter> buildPrefixPathParams(FS functionSpec) {
        return Map.of();
    }

    @Override
    public List<String> getFunctionTags(FS functionSpec) {
        return List.of();
    }

    @Override
    public String getFunctionName(FS functionSpec) {
        return EMPTY;
    }

    @Override
    public String getFunctionDescription(FS functionSpec) {
        return functionSpec.getKey();
    }

    @Override
    public String getFunctionInputJsonSchema(FS functionSpec) {
        return EMPTY;
    }

    @Override
    public String getFunctionOutputJsonSchema(FS functionSpec) {
        return EMPTY;
    }
}

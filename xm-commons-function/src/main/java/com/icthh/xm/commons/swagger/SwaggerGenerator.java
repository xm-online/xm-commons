package com.icthh.xm.commons.swagger;

import com.icthh.xm.commons.swagger.model.SwaggerFunction;
import com.icthh.xm.commons.swagger.model.SwaggerModel;
import com.icthh.xm.commons.swagger.model.SwaggerParameter;

import java.util.Map;

public interface SwaggerGenerator {

    SwaggerModel getSwaggerBody();

    void generateFunction(String prefix, Map<String, SwaggerParameter> pathPrefixParams, SwaggerFunction swaggerFunction);

}

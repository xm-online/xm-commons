package com.icthh.xm.commons.swagger;

import com.icthh.xm.commons.swagger.model.SwaggerModel;

public interface DynamicSwaggerFunctionGenerator {

    SwaggerModel generateSwagger(String baseUrl, String specName);
}

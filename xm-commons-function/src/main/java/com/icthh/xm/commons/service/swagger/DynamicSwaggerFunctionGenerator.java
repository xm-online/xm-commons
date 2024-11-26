package com.icthh.xm.commons.service.swagger;

import com.icthh.xm.commons.service.swagger.model.SwaggerModel;

public interface DynamicSwaggerFunctionGenerator {

    SwaggerModel generateSwagger(String baseUrl);
}

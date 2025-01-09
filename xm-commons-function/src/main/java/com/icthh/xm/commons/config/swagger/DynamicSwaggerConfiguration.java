package com.icthh.xm.commons.config.swagger;

import com.icthh.xm.commons.swagger.model.ServerObject;
import com.icthh.xm.commons.swagger.model.SwaggerInfo;
import com.icthh.xm.commons.swagger.model.TagObject;
import lombok.Data;

import java.util.List;

@Data
public class DynamicSwaggerConfiguration {

    private SwaggerInfo info;
    private List<ServerObject> servers;
    private List<TagObject> tags;

    private List<String> includeTags;
    private List<String> excludeTags;

    // regexps
    private List<String> includePathPatterns;
    private List<String> excludePathPatterns;
    private List<String> includeKeyPatterns;
    private List<String> excludeKeyPatterns;
}

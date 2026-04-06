package com.icthh.xm.commons.lep.groovy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class GStringCustomizer implements JsonMapperBuilderCustomizer {

    private final JacksonModule gStringModule;

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.addModules(gStringModule);
    }
}

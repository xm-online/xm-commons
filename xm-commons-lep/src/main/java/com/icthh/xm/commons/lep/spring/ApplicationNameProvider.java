package com.icthh.xm.commons.lep.spring;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

/**
 * Class created to simplify mock app name and test migration
 */
public class ApplicationNameProvider {
    @Getter
    private final String appName;

    public ApplicationNameProvider(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }
}

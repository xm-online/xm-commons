package com.icthh.xm.commons.logging.util;

import lombok.Getter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BasePackageDetector {

    private static final String XM_BASE_PACKAGE = "com.icthh.xm";

    @Getter
    private final String basePackage;

    public BasePackageDetector(ApplicationContext context) {
        Map<String, Object> candidates = context.getBeansWithAnnotation(SpringBootApplication.class);
        this.basePackage = candidates.isEmpty() ? XM_BASE_PACKAGE : toPackageName(candidates);
    }

    private String toPackageName(Map<String, Object> candidates) {
        String packageName = candidates.values().toArray()[0].getClass().getPackageName();
        if (packageName.indexOf(".ms.") > 0) {
            return packageName.substring(0, packageName.indexOf(".ms."));
        } else {
            return packageName;
        }
    }

}

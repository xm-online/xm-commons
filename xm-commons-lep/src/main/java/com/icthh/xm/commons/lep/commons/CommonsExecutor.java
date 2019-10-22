package com.icthh.xm.commons.lep.commons;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonsExecutor {

    private final CommonsService commonsService;
    private String packagePath = "commons";

    public CommonsExecutor(CommonsService commonsService) {
        this.commonsService = commonsService;
    }

    public CommonsExecutor(CommonsService commonsService, String packagePath) {
        this.commonsService = commonsService;
        this.packagePath = packagePath;
    }

    public Object methodMissing(String name, Object args){
        log.info("Execute commons function: {}.{}() with args: {}", packagePath, name, args);
        return commonsService.execute(packagePath, name, args);
    }

    public Object propertyMissing(String prop) {
        return new CommonsExecutor(commonsService, packagePath + '.' + prop);
    }

}

package com.icthh.xm.commons.lep.commons;

public class Commons {

    private static final String BASE_PACKAGE = "commons";

    private final CommonsExecutor tenant;
    private final CommonsExecutor environment;

    private final CommonsService commonsService;

    public Commons(CommonsService commonsService) {
        this.commonsService = commonsService;
        this.tenant = new CommonsExecutor(commonsService, BASE_PACKAGE + "." + "tenant");
        this.environment = new CommonsExecutor(commonsService, BASE_PACKAGE + "." + "environment");
    }

    public Object propertyMissing(String prop) {
        return new CommonsExecutor(commonsService, BASE_PACKAGE + "." + prop);
    }
}

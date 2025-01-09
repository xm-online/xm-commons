package com.icthh.xm.commons.swagger.model;

import lombok.Data;

import static com.icthh.xm.commons.utils.Constants.SWAGGER_INFO_TITLE;
import static com.icthh.xm.commons.utils.Constants.SWAGGER_INFO_VERSION;

@Data
public class SwaggerInfo {

    private String version = SWAGGER_INFO_VERSION;
    private String title = SWAGGER_INFO_TITLE;
    private String description;
    private String termsOfService;
    private ContactObject contact;
    private LicenseObject license;

    @Data
    public static class ContactObject {
        private String name;
        private String url;
        private String email;
    }

    @Data
    public static class LicenseObject {
        private String name;
        private String url;
        private String identifier;
    }
}

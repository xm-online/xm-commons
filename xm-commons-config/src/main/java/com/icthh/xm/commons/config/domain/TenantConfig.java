//package com.icthh.xm.commons.config.domain;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Configuration class for enable/disable tenant level feature
// * Current properties file representation is /config/tenants/{tenantName}/tenant-profile.yml
// */
//@Data
//public class TenantConfig {
//
//    private Boolean disableDynamicPrivilegesGeneration = false;
//
//    @JsonProperty("entity-functions")
//    private EntityFunctions entityFunctions = new EntityFunctions();
//    @Data
//    public static class EntityFunctions {
//        private Boolean dynamicPermissionCheckEnabled = false;
//        private Boolean validateFunctionInput = false;
//    }
//
//    private DynamicTypeKeyPermission dynamicTypeKeyPermission = new DynamicTypeKeyPermission();
//    @Data
//    public static class DynamicTypeKeyPermission {
//        private Boolean linkDeletion = false;
//    }
//
//    private EntityVersionControl entityVersionControl = new EntityVersionControl();
//    @Data
//    public static class EntityVersionControl {
//        private Boolean enabled = false;
//    }
//
//    private List<MailSetting> mailSettings = new ArrayList<>();
//    @Data
//    public static class MailSetting {
//        private String templateName;
//        private Map<String, String> subject = new HashMap<>();
//        private Map<String, String> from = new HashMap<>();
//    }
//
//    private LepSetting lep = new LepSetting();
//    @Data
//    public static class LepSetting {
//        private Boolean enableInheritanceTypeKey = false;
//    }
//
//    private EntitySpec entitySpec = new EntitySpec();
//    @Data
//    public static class EntitySpec {
//        private Boolean enableDataSpecInheritance = false;
//        private Boolean enableDataFromInheritance = false;
//    }
//}

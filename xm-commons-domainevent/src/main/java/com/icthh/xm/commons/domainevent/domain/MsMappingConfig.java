package com.icthh.xm.commons.domainevent.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MsMappingConfig {

    private OperationMapping operationMapping;

    @Data
    public static class OperationMapping implements Serializable {
        @JsonProperty("GET")
        private MethodMapping getMethod;
        @JsonProperty("POST")
        private MethodMapping posMethod;
        @JsonProperty("PUT")
        private MethodMapping putMethod;
        @JsonProperty("DELETE")
        private MethodMapping deleteMethod;
    }

    @Data
    public static class MethodMapping {
        private List<Mapping> mappings;
    }

    @Data
    public static class Mapping {
        private String urlPattern;
        private String name;
    }
}

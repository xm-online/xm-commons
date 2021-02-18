package com.icthh.xm.commons.domain.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpPrivateConfig {

    @JsonProperty("idp")
    private IdpConfigContainer config;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdpConfigContainer {

        @JsonProperty("clients")
        private List<IdpPrivateClientConfig> clients = new ArrayList<>();

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IdpPrivateClientConfig {

            @JsonProperty("key")
            private String key;

            @JsonProperty("clientSecret")
            private String clientSecret;

            @JsonProperty("scope")
            private Set<String> scope;
        }
    }
}

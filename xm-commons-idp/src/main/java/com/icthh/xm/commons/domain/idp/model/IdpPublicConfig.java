package com.icthh.xm.commons.domain.idp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpPublicConfig {

    @JsonProperty("idp")
    @NotNull
    private IdpConfigContainer config;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NotNull
    public static class IdpConfigContainer {
        @JsonProperty("directLogin")
        @NotNull
        private boolean directLogin;

        @JsonProperty("clients")
        @NotNull
        private List<IdpPublicClientConfig> clients = new ArrayList<>();

        @JsonProperty("features")
        @NotNull
        private Features features;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IdpPublicClientConfig {

            @JsonProperty("key")
            @NotNull
            private String key;

            @JsonProperty("name")
            @NotNull
            private String name;

            @JsonProperty("clientId")
            @NotNull
            private String clientId;

            @JsonProperty("redirectUri")
            @NotNull
            private String redirectUri;

            @JsonProperty("openIdConfig")
            @NotNull
            private OpenIdConfig openIdConfig;

            @Getter
            @Setter
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class OpenIdConfig {

                @JsonProperty("issuer")
                @NotNull
                private String issuer;

                @JsonProperty("authorizationEndpoint")
                @NotNull
                private AuthorizationEndpoint authorizationEndpoint;

                @JsonProperty("tokenEndpoint")
                @NotNull
                private TokenEndpoint tokenEndpoint;

                @JsonProperty("userinfoEndpoint")
                @NotNull
                private UserInfoEndpoint userinfoEndpoint;

                @JsonProperty("endSessionEndpoint")
                @NotNull
                private BaseEndpoint endSessionEndpoint;

                @JsonProperty("jwksEndpoint")
                @NotNull
                private BaseEndpoint jwksEndpoint;

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                @EqualsAndHashCode(callSuper = true)
                public static class AuthorizationEndpoint extends BaseEndpoint {

                    @JsonProperty("responseType")
                    @NotNull
                    private String responseType;

                    @JsonProperty("additionalParams")
                    @NotNull
                    private Map<String, String> additionalParams;

                    @JsonProperty("features")
                    @NotNull
                    private Features features;

                    @Getter
                    @Setter
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class Features {
                        @JsonProperty("state")
                        @NotNull
                        private boolean state;
                    }
                }

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                @EqualsAndHashCode(callSuper = true)
                public static class TokenEndpoint extends BaseEndpoint {
                    @JsonProperty("grantType")
                    @NotNull
                    private String grantType;
                }

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class BaseEndpoint {
                    @JsonProperty("uri")
                    @NotNull
                    private String uri;
                }

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                @EqualsAndHashCode(callSuper = true)
                public static class UserInfoEndpoint extends BaseEndpoint {
                    @JsonProperty("userNameAttributeName")
                    @NotNull
                    private String userNameAttributeName;
                }
            }
        }

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Features {
            @JsonProperty("pkce")
            @NotNull
            private boolean pkce;

            @JsonProperty("stateful")
            @NotNull
            private boolean stateful;

            @JsonProperty("bearirng")
            @NotNull
            private Bearirng bearirng;

            @Getter
            @Setter
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Bearirng {
                @JsonProperty("enabled")
                @NotNull
                private boolean enabled;

                @JsonProperty("idpTokenHeader")
                @NotNull
                private String idpTokenHeader;

                @JsonProperty("xmTokenHeader")
                @NotNull
                private String xmTokenHeader;
            }
        }

    }
}

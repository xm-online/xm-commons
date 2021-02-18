package com.icthh.xm.commons.domain.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpPublicConfig {

    @JsonProperty("idp")
    private IdpConfigContainer config;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdpConfigContainer {
        @JsonProperty("directLogin")
        private boolean directLogin;

        @JsonProperty("clients")
        private List<IdpPublicClientConfig> clients = new ArrayList<>();

        @JsonProperty("features")
        private Features features;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IdpPublicClientConfig {

            @JsonProperty("key")
            private String key;

            @JsonProperty("name")
            private String name;

            @JsonProperty("clientId")
            private String clientId;

            @JsonProperty("redirectUri")
            private String redirectUri;

            @JsonProperty("openIdConfig")
            private OpenIdConfig openIdConfig;

            @Getter
            @Setter
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class OpenIdConfig {

                @JsonProperty("issuer")
                private String issuer;

                @JsonProperty("authorizationEndpoint")
                private AuthorizationEndpoint authorizationEndpoint;

                @JsonProperty("tokenEndpoint")
                private TokenEndpoint tokenEndpoint;

                @JsonProperty("userinfoEndpoint")
                private UserInfoEndpoint userinfoEndpoint;

                @JsonProperty("endSessionEndpoint")
                private BaseEndpoint endSessionEndpoint;

                @JsonProperty("jwksEndpoint")
                private BaseEndpoint jwksEndpoint;

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                @EqualsAndHashCode(callSuper = true)
                public static class AuthorizationEndpoint extends BaseEndpoint {

                    @JsonProperty("responseType")
                    private String responseType;

                    @JsonProperty("additionalParams")
                    private Map<String, String> additionalParams;

                    @JsonProperty("features")
                    private Features features;

                    @Getter
                    @Setter
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class Features {
                        @JsonProperty("state")
                        private boolean state;
                    }
                }

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                @EqualsAndHashCode(callSuper = true)
                public static class TokenEndpoint extends BaseEndpoint {
                    @JsonProperty("grantType")
                    private String grantType;
                }

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class BaseEndpoint {
                    @JsonProperty("uri")
                    private String uri;
                }

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                @EqualsAndHashCode(callSuper = true)
                public static class UserInfoEndpoint extends BaseEndpoint {
                    @JsonProperty("userNameAttributeName")
                    private String userNameAttributeName;
                }
            }
        }

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Features {
            @JsonProperty("pkce")
            private boolean pkce;

            @JsonProperty("stateful")
            private boolean stateful;

            @JsonProperty("bearirng")
            private Bearirng bearirng;

            @Getter
            @Setter
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Bearirng {
                @JsonProperty("enabled")
                private boolean enabled;

                @JsonProperty("idpTokenHeader")
                private String idpTokenHeader;

                @JsonProperty("xmTokenHeader")
                private String xmTokenHeader;
            }
        }

    }
}

package com.icthh.xm.commons.domain.idp;

import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.Features.IdpAccessTokenInclusion;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
@UtilityClass
public class IdpConfigUtils {

    /**
     * Method checks is public config valid for processing.
     *
     * @param idpPublicClientConfig public config for validation
     * @return true if config is valid, otherwise false
     */
    public static boolean isPublicConfigValid(IdpPublicClientConfig idpPublicClientConfig) {
        if (idpPublicClientConfig == null) {
            log.warn("Public idp config not specified.");
            return false;
        }
        if (idpPublicClientConfig.getKey() == null) {
            log.warn("Client key not specified in configuration.");
            return false;
        }
        if (idpPublicClientConfig.getRedirectUri() == null) {
            log.warn("Redirect uri not specified in configuration.");
            return false;
        }
        if (StringUtils.isEmpty(idpPublicClientConfig.getName())) {
            log.warn("Client name not specified in configuration.");
            return false;
        }
        if (StringUtils.isEmpty(idpPublicClientConfig.getClientId())) {
            log.warn("Client id not specified in configuration.");
            return false;
        }
        if (idpPublicClientConfig.getOpenIdConfig() == null) {
            log.warn("openIdConfig section not specified or have lack of configuration.");
            return false;
        }
        if (idpPublicClientConfig.getOpenIdConfig().getAuthorizationEndpoint() == null
            || StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getAuthorizationEndpoint().getUri())) {
            log.warn("Authorization endpoint uri not specified or have lack of configuration.");
            return false;
        }

        if (idpPublicClientConfig.getOpenIdConfig().getTokenEndpoint() == null
            || StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getTokenEndpoint().getUri())) {
            log.warn("Token endpoint uri not specified or have lack of configuration.");
            return false;
        }

        if (idpPublicClientConfig.getOpenIdConfig().getUserinfoEndpoint() == null
            || StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getUserinfoEndpoint().getUri())) {
            log.warn("User info endpoint uri not specified or have lack of configuration.");
            return false;
        }
        if (StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getUserinfoEndpoint().getUserNameAttributeName())) {
            log.warn("User name attribute not specified or have lack of configuration in user info endpoint section.");
            return false;
        }
        if (idpPublicClientConfig.getOpenIdConfig().getJwksEndpoint() == null
            || StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getJwksEndpoint().getUri())) {
            log.warn("jwks endpoint uri not specified or have lack of configuration.");
            return false;
        }
        return true;
    }

    public static boolean isTenantFeaturesConfigValid(IdpPublicConfig.IdpConfigContainer.Features features) {
        if (features == null) {
            log.warn("Features section not specified or have lack of configuration." );
            return false;
        }

        IdpAccessTokenInclusion idpAccessTokenInclusion = features.getIdpAccessTokenInclusion();

        if (idpAccessTokenInclusion == null) {
            log.warn("features.idpAccessTokenInclusion section not specified." );
            return false;
        }
        if (StringUtils.isEmpty(idpAccessTokenInclusion.getIdpTokenHeader())) {
            log.warn("features.idpAccessTokenInclusion.idpTokenHeader option not specified." );
            return false;
        }
        if (StringUtils.isEmpty(idpAccessTokenInclusion.getXmTokenHeader())) {
            log.warn("features.idpAccessTokenInclusion.xmTokenHeader option not specified." );
            return false;
        }
        return true;
    }

    /**
     * Method checks is private valid for processing.
     *
     * @param idpPrivateClientConfig private config for validation
     * @return true if config is valid, otherwise false
     */
    public static boolean isPrivateConfigValid(IdpPrivateClientConfig idpPrivateClientConfig) {
        if (idpPrivateClientConfig == null) {
            log.warn("private idp config not specified.");

            return false;
        }
        if (idpPrivateClientConfig.getClientSecret() == null) {
            log.warn("client secret not specified in configuration.");
            return false;
        }
        if (CollectionUtils.isEmpty(idpPrivateClientConfig.getScope())) {
            log.warn("idp scopes not specified in configuration.");
            return false;
        }
        return true;
    }
}

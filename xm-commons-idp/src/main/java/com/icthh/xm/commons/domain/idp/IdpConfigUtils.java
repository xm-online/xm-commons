package com.icthh.xm.commons.domain.idp;

import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
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
     * @param tenantKey             tenant name
     * @param idpPublicClientConfig public config for validation
     * @return true if config is valid, otherwise false
     */
    // FIXME: suggest removing tenant key from the params.
    // FIXME: do we really need this method? or we may validate config for example with bean Validation? Just to think.
    public static boolean isPublicConfigValid(String tenantKey, IdpPublicClientConfig idpPublicClientConfig) {
        if (idpPublicClientConfig == null) {
            log.info("For tenant [{}] public idp config not specified.", tenantKey);
            return false;
        }
        if (idpPublicClientConfig.getKey() == null) {
            log.info("For tenant [{}] key not specified in configuration.", tenantKey);
            return false;
        }
        if (idpPublicClientConfig.getRedirectUri() == null) {
            log.info("For tenant [{}] redirect uri not specified in configuration.", tenantKey);
            return false;
        }
        if (StringUtils.isEmpty(idpPublicClientConfig.getName())) {
            log.info("For tenant [{}] client name not specified in configuration.", tenantKey);
            return false;
        }
        if (StringUtils.isEmpty(idpPublicClientConfig.getClientId())) {
            log.info("For tenant [{}] client id not specified in configuration.", tenantKey);
            return false;
        }
        if (idpPublicClientConfig.getOpenIdConfig() == null) {
            log.info("For tenant [{}] openIDConfig section not specified or have lack of configuration.", tenantKey);
            return false;
        }
        if (idpPublicClientConfig.getOpenIdConfig().getAuthorizationEndpoint() == null
            || StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getAuthorizationEndpoint().getUri())) {
            log.info("For tenant [{}] authorization endpoint uri not specified or have lack of configuration.", tenantKey);
            return false;
        }

        if (idpPublicClientConfig.getOpenIdConfig().getTokenEndpoint() == null
            || StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getTokenEndpoint().getUri())) {
            log.info("For tenant [{}] token endpoint uri not specified or have lack of configuration.", tenantKey);
            return false;
        }

        if (idpPublicClientConfig.getOpenIdConfig().getUserinfoEndpoint() == null
            || StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getUserinfoEndpoint().getUri())) {
            log.info("For tenant [{}] user info endpoint uri not specified or have lack of configuration.", tenantKey);
            return false;
        }
        if (StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getUserinfoEndpoint().getUserNameAttributeName())) {
            log.info("For tenant [{}] user name attribute not specified or "
                + "have lack of configuration in user info endpoint section.", tenantKey);
            return false;
        }
        if (idpPublicClientConfig.getOpenIdConfig().getJwksEndpoint() == null
            || StringUtils.isEmpty(idpPublicClientConfig.getOpenIdConfig().getJwksEndpoint().getUri())) {
            log.info("For tenant [{}] jwks endpoint uri not specified or have lack of configuration.", tenantKey);
            return false;
        }
        return true;
    }

    public static boolean isTenantFeaturesConfigValid(IdpPublicConfig.IdpConfigContainer.Features features) {
        if (features == null) {
            log.info("Features section not specified or have lack of configuration." );
            return false;
        }
        if (features.getBearirng() == null) {
            log.info("features.bearing section not specified." );
            return false;
        }
        if (StringUtils.isEmpty(features.getBearirng().getIdpTokenHeader())) {
            log.info("features.bearing.idpTokenHeader option not specified." );
            return false;
        }
        if (StringUtils.isEmpty(features.getBearirng().getXmTokenHeader())) {
            log.info("features.bearing.xmTokenHeader option not specified." );
            return false;
        }
        return true;
    }

    /**
     * Method checks is private valid for processing.
     *
     * @param tenantKey              tenant name
     * @param idpPrivateClientConfig private config for validation
     * @return true if config is valid, otherwise false
     */
    public static boolean isPrivateConfigValid(String tenantKey, IdpPrivateClientConfig idpPrivateClientConfig) {
        if (idpPrivateClientConfig == null) {
            log.info("For tenant [{}] private idp config not specified.", tenantKey);

            return false;
        }
        if (idpPrivateClientConfig.getClientSecret() == null) {
            log.info("For tenant [{}] client secret not specified in configuration.", tenantKey);
            return false;
        }
        if (CollectionUtils.isEmpty(idpPrivateClientConfig.getScope())) {
            log.info("For tenant [{}] idp scopes not specified in configuration.", tenantKey);
            return false;
        }
        return true;
    }
}

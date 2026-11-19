package com.icthh.xm.commons.secret;

import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.lep.api.LepBaseKey;
import com.icthh.xm.commons.lep.api.LepEngine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adds {@code secrets} to lepContext. */
@Component
public class SecretLepContext implements LepAdditionalContext<LepSecrets> {

    private final SecretService secretService;
    /** Service patterns do not change after start: copied once, not on every LEP call. */
    private final List<String> servicePatterns;
    private final TenantSecretsConfiguration tenantSecrets;

    SecretLepContext(SecretService secretService, SecretProperties properties,
                     TenantSecretsConfiguration tenantSecrets) {
        this.secretService = secretService;
        this.servicePatterns = List.copyOf(properties.getLepReadBlacklist());
        this.tenantSecrets = tenantSecrets;
    }

    @Override
    public String additionalContextKey() {
        return SecretsField.FIELD_NAME;
    }

    @Override
    public LepSecrets additionalContextValue() {
        return new LepSecrets(secretService, servicePatterns, tenantSecrets, "lep");
    }

    @Override
    public Optional<LepSecrets> additionalContextValue(BaseLepContext lepContext, LepEngine lepEngine,
                                                       TargetProceedingLep lepMethod) {
        LepBaseKey key = lepMethod.getLepBaseKey();
        String caller = "lep:" + key.getGroup() + ":" + key.getBaseKey();
        return Optional.of(new LepSecrets(secretService, servicePatterns, tenantSecrets, caller));
    }

    @Override
    public Class<? extends LepAdditionalContextField> fieldAccessorInterface() {
        return SecretsField.class;
    }

    public interface SecretsField extends LepAdditionalContextField {
        String FIELD_NAME = "secrets";

        default LepSecrets getSecrets() {
            return (LepSecrets) get(FIELD_NAME);
        }
    }
}

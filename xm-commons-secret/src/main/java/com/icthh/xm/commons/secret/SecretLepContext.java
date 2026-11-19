package com.icthh.xm.commons.secret;

import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContext;
import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.lep.api.LepBaseKey;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adds {@code secrets} to lepContext. */
@Component
@RequiredArgsConstructor
public class SecretLepContext implements LepAdditionalContext<LepSecrets> {

    private final SecretService secretService;
    private final SecretProperties properties;
    private final TenantSecretsConfiguration tenantSecrets;

    @Override
    @IgnoreLogginAspect
    public String additionalContextKey() {
        return SecretsField.FIELD_NAME;
    }

    @Override
    @IgnoreLogginAspect
    public LepSecrets additionalContextValue() {
        return new LepSecrets(secretService, List.copyOf(properties.getLepReadBlacklist()), tenantSecrets, "lep");
    }

    @Override
    @IgnoreLogginAspect
    public Optional<LepSecrets> additionalContextValue(BaseLepContext lepContext, LepEngine lepEngine,
                                                       TargetProceedingLep lepMethod) {
        LepBaseKey key = lepMethod.getLepBaseKey();
        String caller = "lep:" + key.getGroup() + ":" + key.getBaseKey();
        return Optional.of(new LepSecrets(secretService, List.copyOf(properties.getLepReadBlacklist()), tenantSecrets, caller));
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

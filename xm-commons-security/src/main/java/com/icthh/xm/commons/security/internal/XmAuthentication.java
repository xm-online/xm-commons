package com.icthh.xm.commons.security.internal;

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;

public class XmAuthentication extends AbstractAuthenticationToken {

    private final XmAuthenticationDetails details;

    private Object credentials;

    public XmAuthentication(XmAuthenticationDetails details, Object credentials) {
        super(null);
        this.details = details;
        this.credentials = credentials;
        setAuthenticated(false);
    }

    public XmAuthentication(XmAuthenticationDetails details, Object credentials,
                            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.details = details;
        this.credentials = credentials;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public String getPrincipal() {
        return isNoneBlank(details.getUserKey()) ? details.getUserKey() : details.getClientId();
    }

    public boolean isClientOnly() {
        return details.getUserKey() == null && details.getUserName() == null;
    }

    @Override
    @Nullable
    public XmAuthenticationDetails getDetails() {
        return this.details;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        Assert.isTrue(!isAuthenticated,
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}

package com.icthh.xm.commons.security.internal;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The {@link SpringSecurityXmAuthenticationContextHolder} class.
 */
public class SpringSecurityXmAuthenticationContextHolder implements XmAuthenticationContextHolder {

    /**
     * {@inheritDoc}
     */
    @Override
    public XmAuthenticationContext getContext() {
        return new SpringSecurityXmAuthenticationContext(SecurityContextHolder.getContext());
    }

}

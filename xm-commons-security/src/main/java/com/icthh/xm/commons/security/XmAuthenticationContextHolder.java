package com.icthh.xm.commons.security;

/**
 * The {@link XmAuthenticationContextHolder} class.
 */
public interface XmAuthenticationContextHolder {

    /**
     * Obtain the current {@code XmAuthenticationContext}.
     *
     * @return the xm authentication context (never {@code null})
     */
    XmAuthenticationContext getContext();

}

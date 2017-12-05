package com.icthh.xm.commons.lep;

import org.codehaus.groovy.tools.shell.util.NoExitSecurityManager;

/**
 * The {@link XmLepResourceSecurityManager} class.
 */
// https://stackoverflow.com/questions/15868534/why-java-security-manager-doesnt-forbid-neither-creating-new-thread-nor-start
public class XmLepResourceSecurityManager extends NoExitSecurityManager {

    public XmLepResourceSecurityManager() {
        this(System.getSecurityManager());
    }

    public XmLepResourceSecurityManager(final SecurityManager parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("Use of java.lang.Runtime#exec(...) is forbidden!");
    }

}

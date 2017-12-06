package com.icthh.xm.commons.lep;

/**
 * The {@link XmLepScriptConstants} class.
 */
public final class XmLepScriptConstants {

    public static final String BINDING_KEY_AUTH_CONTEXT = "authContext";
    public static final String BINDING_KEY_TENANT_CONTEXT = "tenantContext";
    public static final String BINDING_KEY_IN_ARGS = "inArgs";
    public static final String BINDING_KEY_LEP = "lep";
    public static final String BINDING_KEY_RETURNED_VALUE = "returnedValue";
    public static final String BINDING_KEY_METHOD_RESULT = "methodResult";

    public static final String BINDING_VAR_LEP_SCRIPT_CONTEXT = "lepContext";

    private XmLepScriptConstants() {
        throw new UnsupportedOperationException("Prevent creation for constructor utils class");
    }

}

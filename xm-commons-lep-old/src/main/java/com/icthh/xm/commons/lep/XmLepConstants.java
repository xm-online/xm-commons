package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.Version;
import com.icthh.xm.lep.api.commons.GroupMode;
import com.icthh.xm.lep.api.commons.GroupMode.Builder;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;

/**
 * The {@link XmLepConstants} class.
 */
public final class XmLepConstants {

    /**
     * URL delimiter constant.
     */
    public static final String URL_DELIMITER = "/";

    /**
     * URL net path constant.
     */
    public static final String URL_NET_PATH_DELIMITER = "//";

    /**
     * LEP key separator.
     */
    public static final String EXTENSION_KEY_SEPARATOR = ".";

    /**
     * LEP key separator regular expression.
     */
    public static final String EXTENSION_KEY_SEPARATOR_REGEXP = "\\.";

    /**
     * Group mode for {@link SeparatorSegmentedLepKey}.
     */
    public static final GroupMode EXTENSION_KEY_GROUP_MODE = new Builder().prefixExcludeLastSegmentsAndIdIncludeGroup(1)
        .build();

    /**
     * LEP resource version for unused value.
     */
    public static final Version UNUSED_RESOURCE_VERSION = null;

    /**
     * Thread context variable name for tenant context.
     */
    public static final String THREAD_CONTEXT_KEY_TENANT_CONTEXT = "tenantContext";

    /**
     * Thread context variable name for authentication context.
     */
    public static final String THREAD_CONTEXT_KEY_AUTH_CONTEXT = "authContext";

    /**
     * LEP script extension separator.
     */
    public static final String SCRIPT_EXTENSION_SEPARATOR = ".";

    /**
     * LEP script name separator.
     */
    public static final String SCRIPT_NAME_SEPARATOR = "$$";

    /**
     * LEP script name separator regular expression.
     */
    public static final String SCRIPT_NAME_SEPARATOR_REGEXP = "\\$\\$";


    /**
     * Groovy script name extension.
     */
    public static final String SCRIPT_EXTENSION_GROOVY = "groovy";

    /**
     * Groovy file name extension.
     */
    public static final String FILE_EXTENSION_GROOVY = SCRIPT_EXTENSION_SEPARATOR + SCRIPT_EXTENSION_GROOVY;

    /**
     * LEP script any type-key segment name.
     */
    public static final String SCRIPT_ANY_TYPE_KEY = "_ANY_";

    private XmLepConstants() {
        throw new UnsupportedOperationException("Prevent creation for constructor utils class");
    }

}

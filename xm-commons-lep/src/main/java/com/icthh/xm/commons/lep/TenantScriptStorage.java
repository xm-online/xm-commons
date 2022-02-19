package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * The {@link TenantScriptStorage} class.
 */
public enum TenantScriptStorage {

    CLASSPATH, XM_MS_CONFIG, FILE;

    /**
     * URL prefix for environment commons.
     */
    public static final String URL_PREFIX_COMMONS_ENVIRONMENT = "/commons/environment";

    /**
     * URL prefix for tenant commons.
     */
    public static final String URL_PREFIX_COMMONS_TENANT = "/commons/tenant";

}

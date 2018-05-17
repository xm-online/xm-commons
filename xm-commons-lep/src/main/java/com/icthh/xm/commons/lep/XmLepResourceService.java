package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;
import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepResource;
import com.icthh.xm.lep.api.LepResourceDescriptor;
import com.icthh.xm.lep.api.LepResourceKey;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.LepResourceType;
import com.icthh.xm.lep.api.Version;
import com.icthh.xm.lep.api.commons.DefaultLepResourceDescriptor;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.script.InputStreamSupplier;
import com.icthh.xm.lep.script.ScriptLepResource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@link XmLepResourceService} class.
 */
@Slf4j
public class XmLepResourceService implements LepResourceService {

    private static final Pattern SCRIPT_TYPE_PATTERN = Pattern
        .compile("^.*\\Q" + XmLepConstants.SCRIPT_NAME_SEPARATOR + "\\E(.*?)\\Q"
                     + XmLepConstants.FILE_EXTENSION_GROOVY + "\\E$");

    private final TenantScriptStorage tenantScriptStorage;
    private final String appName;
    @Getter
    private final ResourceLoader routerResourceLoader;

    public XmLepResourceService(String appName,
                                TenantScriptStorage tenantScriptStorage,
                                ResourceLoader routerResourceLoader) {
        this.appName = Objects.requireNonNull(appName, "appName can't be null");
        this.tenantScriptStorage = Objects.requireNonNull(tenantScriptStorage,
                                                          "tenantScriptStorage can't be null");
        this.routerResourceLoader = routerResourceLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceExists(ContextsHolder contextsHolder, LepResourceKey resourceKey) {
        return getScriptResource(contextsHolder, resourceKey).exists();
    }

    // lep:/some/group/<script_name>$<entityType>$<from_state_name>$<to_state_name>$<script_type>.groovy
    // lep:/com/icthh/lep/<script_name>$<entityType>$<state>$<script_type>.groovy

    /**
     * {@inheritDoc}
     */
    @Override
    public LepResourceDescriptor getResourceDescriptor(ContextsHolder contextsHolder, LepResourceKey resourceKey) {
        Objects.requireNonNull(resourceKey, "resourceKey can't be null");
        Resource scriptResource = getScriptResource(contextsHolder, resourceKey);
        if (!scriptResource.exists()) {
            log.debug("No LEP resource for key {}", resourceKey);
            return null;
        }
        return getLepResourceDescriptor(resourceKey, scriptResource);
    }

    private LepResourceDescriptor getLepResourceDescriptor(LepResourceKey resourceKey,
                                                           Resource scriptResource) {
        // get script modification time
        Instant modificationTime;
        try {
            modificationTime = Instant.ofEpochMilli(scriptResource.lastModified());
        } catch (IOException e) {
            throw new IllegalStateException(
                "Error while getting script resource modification time: "
                    + e.getMessage(),
                e);
        }

        // build descriptor
        return new DefaultLepResourceDescriptor(getResourceType(resourceKey), resourceKey,
                                                Instant.EPOCH, modificationTime);
    }

    private static LepResourceType getResourceType(LepResourceKey resourceKey) {
        String id = resourceKey.getId();
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Resource key id cant be blank");
        }

        if (id.endsWith(XmLepConstants.SCRIPT_EXTENSION_GROOVY)) {
            return XmLepResourceType.GROOVY;
        }

        throw new IllegalStateException(
            "Unsupported LEP resource script type for key: " + resourceKey.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LepResource getResource(ContextsHolder contextsHolder, LepResourceKey resourceKey) {
        Objects.requireNonNull(resourceKey, "resourceKey can't be null");

        log.debug("Getting LEP resource for key {}", resourceKey);

        final Resource scriptResource = getScriptResource(contextsHolder, resourceKey);
        if (!scriptResource.exists()) {
            log.debug("No LEP resource for key {}", resourceKey);
            return null;
        }

        // build descriptor
        LepResourceDescriptor descriptor = getLepResourceDescriptor(resourceKey, scriptResource);
        log.debug("LEP resource for key {} found, descriptor: {}", resourceKey, descriptor);

        return new ScriptLepResource(descriptor, ScriptLepResource.DEFAULT_ENCODING,
                                     new InputStreamSupplier() {

                                         /**
                                          * {@inheritDoc}
                                          */
                                         @Override
                                         public InputStream getInputStream() throws IOException {
                                             return scriptResource.getInputStream();
                                         }

                                     });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LepResource saveResource(ContextsHolder contextsHolder, LepKey extensionKey, LepResource resource) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Version> getResourceVersions(ContextsHolder contextsHolder, LepResourceKey resourceKey) {
        return Collections.emptyList();
    }

    private Resource getScriptResource(ContextsHolder contextsHolder, LepResourceKey resourceKey) {
        String location = getResourceLocation(contextsHolder, resourceKey);
        log.trace("LEP resource with key '{}' resolved to location '{}'", resourceKey, location);

        return routerResourceLoader.getResource(location);
    }

    private String getResourceLocation(ContextsHolder contextsHolder, LepResourceKey resourceKey) {
        if (!(resourceKey instanceof UrlLepResourceKey)) {
            throw new IllegalArgumentException("Unsupported LEP resource key type: "
                                                   + resourceKey.getClass().getCanonicalName());
        }

        UrlLepResourceKey urlKey = UrlLepResourceKey.class.cast(resourceKey);
        String path = urlKey.getUrlResourcePath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // get script type ? '$default.groovy'
        Matcher matcher = SCRIPT_TYPE_PATTERN.matcher(path);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                "Can't find script type in LEP resource key: " + resourceKey);
        }
        String type = matcher.group(1);

        // is default script
        if (XmLepResourceSubType.DEFAULT.getName().equals(type)) {
            return getDefaultScriptLocation(path);
        } else {
            // tenant script
            return getTenantScriptLocation(path, contextsHolder);
        }
    }

    private String getDefaultScriptLocation(String path) {
        // exclude type
        int beforeTypeIndex = path.lastIndexOf(XmLepConstants.SCRIPT_NAME_SEPARATOR);
        int scriptExtIndex = path.lastIndexOf(XmLepConstants.SCRIPT_EXTENSION_SEPARATOR);
        String pathForDefault = path.substring(0, beforeTypeIndex)
            + path.substring(scriptExtIndex);
        return CLASSPATH_URL_PREFIX + "/lep/default" + pathForDefault;
    }

    private String getTenantScriptLocation(String path, ContextsHolder contextsHolder) {
        String tenantKey = LepContextUtils.getTenantKey(contextsHolder);
        switch (tenantScriptStorage) {
            case CLASSPATH:
                return CLASSPATH_URL_PREFIX + "/lep/custom/" + tenantKey.toLowerCase() + path;

            case XM_MS_CONFIG:
                return XM_MS_CONFIG_URL_PREFIX + "/config/tenants/" + tenantKey.toUpperCase() + "/"
                    + appName + "/lep" + path;

            case FILE: {
                String lepDir = Paths.get(FileSystemUtils.APP_HOME_DIR, "config", "tenants",
                                          tenantKey.toUpperCase(), appName, "lep").toString();
                return "file://" + lepDir + FilenameUtils.separatorsToSystem(path);
            }

            default:
                throw new IllegalStateException("Unsupported tenant script storage type: "
                                                    + tenantScriptStorage);
        }
    }

}

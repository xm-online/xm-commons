package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.XmLepConstants.EXTENSION_KEY_SEPARATOR_REGEXP;
import static com.icthh.xm.commons.lep.XmLepConstants.SCRIPT_EXTENSION_GROOVY;
import static com.icthh.xm.commons.lep.XmLepConstants.SCRIPT_EXTENSION_SEPARATOR;
import static com.icthh.xm.commons.lep.XmLepConstants.SCRIPT_NAME_SEPARATOR_REGEXP;
import static com.icthh.xm.commons.lep.XmLepConstants.URL_DELIMITER;

import com.icthh.xm.lep.api.Extension;
import com.icthh.xm.lep.api.ExtensionGroup;
import com.icthh.xm.lep.api.ExtensionResourceDescriptor;
import com.icthh.xm.lep.api.ExtensionService;
import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepResourceKey;
import com.icthh.xm.lep.api.Version;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * The {@link XmExtensionService} class.
 */
@Slf4j
public class XmExtensionService implements ExtensionService {

    private static final String[] EMPTY_GROUP_SEGMENTS = new String[0];


    /**
     * Return composite resource key for specified extension key.
     * If extension has no resource or extension with specified key doesn't exist the method
     * returns {@code null}.
     *
     * @param extensionKey             the extension key
     * @param extensionResourceVersion ignored in current implementation
     * @return composite resource key or {@code null} if not found
     */
    @Override
    public UrlLepResourceKey getResourceKey(LepKey extensionKey, Version extensionResourceVersion) {
        if (extensionKey == null) {
            return null;
        }

        LepKey groupKey = extensionKey.getGroupKey();
        String extensionKeyId = extensionKey.getId();
        String extensionName;
        String[] groupSegments;
        if (groupKey == null) {
            groupSegments = EMPTY_GROUP_SEGMENTS;
            extensionName = extensionKeyId;
        } else {
            groupSegments = groupKey.getId().split(EXTENSION_KEY_SEPARATOR_REGEXP);

            // remove group from id
            extensionName = extensionKeyId.replace(groupKey.getId(), "");
            if (extensionName.startsWith(XmLepConstants.EXTENSION_KEY_SEPARATOR)) {
                extensionName = extensionName.substring(XmLepConstants.EXTENSION_KEY_SEPARATOR.length());
            }
        }

        // change script name: capitalize first character & add script type extension
        String scriptName = extensionName.replaceAll(EXTENSION_KEY_SEPARATOR_REGEXP, SCRIPT_NAME_SEPARATOR_REGEXP);
        scriptName = StringUtils.capitalize(scriptName);

        String urlPath = URL_DELIMITER;
        if (groupSegments.length > 0) {
            urlPath += String.join(URL_DELIMITER, groupSegments) + URL_DELIMITER;
        }
        urlPath += scriptName + SCRIPT_EXTENSION_SEPARATOR + SCRIPT_EXTENSION_GROOVY;

        // TODO if possible add check that returned resourceKey contains resources (for speed up executor reaction)
        // Check example : return isResourceExists(resourceKey) ? resourceKey : null;
        UrlLepResourceKey urlLepResourceKey = UrlLepResourceKey.valueOfUrlResourcePath(urlPath);

        if (extensionResourceVersion == null) {
            log.debug("LEP extension key: '{}' translated to --> composite resource key: '{}'", extensionKey, urlLepResourceKey);
        } else {
            log.debug("LEP extension 'key: {}, v{}' translated to --> composite resource key: '{}'", extensionKey,
                      extensionResourceVersion, urlLepResourceKey);
        }
        return urlLepResourceKey;
    }

    @Override
    public List<ExtensionResourceDescriptor> getExtensionResourceDescriptors() {
        throw new UnsupportedOperationException("not used yet");
    }

    @Override
    public ExtensionResourceDescriptor getExtensionResourceDescriptor(LepKey extensionKey) {
        throw new UnsupportedOperationException("not used yet");
    }

    @Override
    public Extension getExtension(LepKey key) {
        throw new UnsupportedOperationException("not used yet");
    }

    @Override
    public List<Extension> getExtensions() {
        throw new UnsupportedOperationException("not used yet");
    }

    @Override
    public List<ExtensionGroup> getExtensionGroups(Set<LepKey> groupKeys) {
        throw new UnsupportedOperationException("not used yet");
    }

    @Override
    public void deleteExtension(LepKey key) {
        throw new UnsupportedOperationException("not used yet");
    }

}

package com.icthh.xm.commons.lep;

import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.storage.FileTenantScriptPathResolver;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepResource;
import com.icthh.xm.lep.api.LepResourceKey;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link XmLepResourceServiceFileUnitTest} class.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {EmptyTestConfig.class})
public class XmLepResourceServiceFileUnitTest {

    private static final String APP_NAME = "test-app";
    private static final String TENANT_KEY_VALUE = "test";
    private static final String SCRIPT_CLASSPATH_URL = "classpath:/lep/test/resource/service/Script$$before.groovy";
    private static final String USER_HOME = "user.home";

    @Rule
    public ProvideSystemProperty properties;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Autowired
    private ResourceLoader resourceLoader;

    private LepResourceService resourceService;

    private File testScriptDir;

    @Before
    public void before() throws IOException {

        // init temp folder
        testScriptDir = folder.newFolder("home", "xm-online", "config", "tenants",
                                         TENANT_KEY_VALUE.toUpperCase(), APP_NAME,
                                         "lep", "resource", "service");


        // init system property
        File baseDir = Paths.get(folder.getRoot().toPath().toString(), "home").toFile();
        String baseDirPath = Paths.get(baseDir.getAbsolutePath(), "xm-online").toString();

        // copy script from classpath to file system tmp folder
        File scriptFile = Paths.get(testScriptDir.getAbsolutePath(), "Script$$before.groovy").toFile();
        if (!scriptFile.createNewFile()) {
            throw new IllegalStateException("Can't create file: " + scriptFile.getAbsolutePath());
        }
        InputStream scriptIn = resourceLoader.getResource(SCRIPT_CLASSPATH_URL).getInputStream();
        Files.copy(scriptIn, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        scriptIn.close();

        // init resource service
        Map<String, ResourceLoader> urlPrefixToResourceLoader = new HashMap<>();
        urlPrefixToResourceLoader.put("file:", new XmFileSystemResourceLoader(new FileSystemResourceLoader(), new TenantAliasService(), APP_NAME));
        RouterResourceLoader routerResourceLoader = new RouterResourceLoader(urlPrefixToResourceLoader);
        resourceService = new XmLepResourceService(APP_NAME, new FileTenantScriptPathResolver(baseDirPath), routerResourceLoader);
    }

    @Test
    public void lepKeySuccessFileResolving() throws IOException {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant(TENANT_KEY_VALUE);

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey.valueOfUrlResourcePath("/resource/service/Script$$before.groovy");

        LepResource resource = resourceService.getResource(holder, resourceKey);
        assertNotNull(resource);
        assertFalse(resource.isComposite());
        assertNotNull(resource.getValue(InputStream.class));

        String scriptText = IOUtils.toString(resource.getValue(InputStream.class), Charset.forName("UTF-8"));
        scriptText = StringUtils.normalizeSpace(scriptText);
        String expected = "return 'Hello from Script$$before.groovy'\n";
        expected = StringUtils.normalizeSpace(expected);
        assertEquals(expected, scriptText);
    }

}

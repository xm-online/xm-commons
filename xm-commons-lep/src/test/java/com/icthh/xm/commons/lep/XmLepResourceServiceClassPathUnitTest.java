package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.TenantScriptStorage.CLASSPATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepResource;
import com.icthh.xm.lep.api.LepResourceDescriptor;
import com.icthh.xm.lep.api.LepResourceKey;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;

/**
 * The {@link XmLepResourceServiceClassPathUnitTest} class.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {EmptyTestConfig.class})
public class XmLepResourceServiceClassPathUnitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Autowired
    private ResourceLoader resourceLoader;

    private LepResourceService resourceService;

    @Before
    public void before() {
        resourceService = new XmLepResourceService("test-app", CLASSPATH, resourceLoader);
    }

    @Test
    public void getResourceDescriptorThrowsNpeOnNullKey() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("resourceKey can't be null");

        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");
        resourceService.getResourceDescriptor(holder, null);
    }

    @Test
    public void getResourceDescriptorReturnNullOnNonExistingKey() {
        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/_QWE_Script_XYZ$$type.groovy");

        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        LepResourceDescriptor resourceDescriptor = resourceService.getResourceDescriptor(holder, resourceKey);
        assertNull(resourceDescriptor);
    }

    private static void assertValidClasspathResourceDescriptor(
        LepResourceKey resourceKey,
        LepResourceDescriptor resourceDescriptor) {
        assertNotNull(resourceDescriptor);
        assertEquals(XmLepResourceType.GROOVY, resourceDescriptor.getType());
        assertEquals(resourceKey, resourceDescriptor.getKey());
        assertEquals(Instant.EPOCH, resourceDescriptor.getCreationTime());
        assertNotNull(resourceDescriptor.getModificationTime());
    }

    @Test
    public void getValidBeforeResourceDescriptor() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter$$before.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService.getResourceDescriptor(holder, resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    @Test
    public void getValidAroundResourceDescriptor() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithAround$$around.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService.getResourceDescriptor(holder, resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    @Test
    public void getValidTenantResourceDescriptor() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithTenant$$tenant.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService.getResourceDescriptor(holder, resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    @Test
    public void getValidDefaultResourceDescriptor() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/Script$$default.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService.getResourceDescriptor(holder, resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    @Test
    public void getValidAfterResourceDescriptor() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter$$after.groovy");

        LepResourceDescriptor resourceDescriptor = resourceService.getResourceDescriptor(holder, resourceKey);
        assertValidClasspathResourceDescriptor(resourceKey, resourceDescriptor);
    }

    // =========== RESOURCES TESTS

    @Test
    public void getResourceThrowsNpeOnNullKey() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("resourceKey can't be null");

        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super puper");

        resourceService.getResource(holder, null);
    }

    @Test
    public void getResourceReturnNullOnNonExistingKey() {
        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/Abc_Script_Xyz$$some-type.groovy");

        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        LepResource resource = resourceService.getResource(holder, resourceKey);
        assertNull(resource);
    }

    private static void assertValidClasspathResource(LepResourceKey resourceKey,
                                                     LepResource resource,
                                                     String expectedScriptText) {
        assertNotNull(resource);
        assertNotNull(resource.getDescriptor());

        assertValidClasspathResourceDescriptor(resourceKey, resource.getDescriptor());

        String scriptText = resource.getValue(String.class);
        assertTrue("Expected contains script text: [" + expectedScriptText + "], actual text: ["
                       + scriptText + "]", scriptText.startsWith(expectedScriptText));
    }

    @Test
    public void getValidBeforeResource() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter$$before.groovy");

        LepResource resource = resourceService.getResource(holder, resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                                     "return \"ScriptWithBeforeAfter.groovy before, tenant: super\"");
    }

    @Test
    public void getValidAroundResource() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithAround$$around.groovy");

        LepResource resource = resourceService.getResource(holder, resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                                     "return \"ScriptWithAround.groovy around, tenant: super\"");
    }

    @Test
    public void getValidTenantResource() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithTenant$$tenant.groovy");

        LepResource resource = resourceService.getResource(holder, resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                                     "return \"ScriptWithTenant.groovy tenant, tenant: super\"");
    }

    @Test
    public void getValidDefaultResource() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/Script$$default.groovy");

        LepResource resource = resourceService.getResource(holder, resourceKey);
        assertValidClasspathResource(resourceKey, resource, "return \"Script.groovy default\"");
    }

    @Test
    public void getValidAfterResource() {
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter$$after.groovy");

        LepResource resource = resourceService.getResource(holder, resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                                     "return \"ScriptWithBeforeAfter.groovy after, tenant: super\"");
    }

    @Test
    public void getValidTenantResourceForTenant() {
        // build resource key
        LepResourceKey resourceKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithTenant$$tenant.groovy");

        // TENANT 'super'
        ContextsHolder holder = ContextHolderUtils.buildWithTenant("super");

        LepResource resource = resourceService.getResource(holder, resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                                     "return \"ScriptWithTenant.groovy tenant, tenant: super\"");

        // TENANT 'test'
        holder = ContextHolderUtils.buildWithTenant("test");

        resource = resourceService.getResource(holder, resourceKey);
        assertValidClasspathResource(resourceKey, resource,
                                     "return \"ScriptWithTenant.groovy tenant, tenant: test\"");
    }

}

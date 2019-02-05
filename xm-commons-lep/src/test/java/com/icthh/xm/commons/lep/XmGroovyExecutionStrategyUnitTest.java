package com.icthh.xm.commons.lep;

import static com.icthh.xm.commons.lep.TenantScriptStorage.CLASSPATH;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.ContextsHolder;
import com.icthh.xm.lep.api.LepInvocationCauseException;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.MethodSignature;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.lep.api.commons.UrlLepResourceKey;
import com.icthh.xm.lep.groovy.DefaultScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.groovy.GroovyScriptRunner;
import com.icthh.xm.lep.groovy.LazyGroovyScriptEngineProviderStrategy;
import com.icthh.xm.lep.groovy.ScriptNameLepResourceKeyMapper;
import com.icthh.xm.lep.groovy.StrategyGroovyLepExecutor;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The {@link XmGroovyExecutionStrategyUnitTest} class.
 * <br>
 * SpringRunner - needed only for simplify ResourceLoader injection.
 * EmptyTestConfig - needed only for init Spring Application Context.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {EmptyTestConfig.class})
public class XmGroovyExecutionStrategyUnitTest {

    @Autowired
    private ResourceLoader resourceLoader;

    private XmGroovyExecutionStrategy execStrategy;

    private Supplier<GroovyScriptRunner> resourceExecutorSupplier;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        this.execStrategy = new XmGroovyExecutionStrategy();

        ScriptNameLepResourceKeyMapper mapper = new DefaultScriptNameLepResourceKeyMapper();
        LazyGroovyScriptEngineProviderStrategy providerStrategy = new LazyGroovyScriptEngineProviderStrategy(
            mapper);
        GroovyScriptRunner scriptRunner = new StrategyGroovyLepExecutor(mapper, providerStrategy,
                                                                        execStrategy);
        resourceExecutorSupplier = () -> scriptRunner;
    }

    private LepManagerService buildLepManagerService(String tenantKeyValue) {

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenantKeyValue)));

        // AuthenticationContext
        XmAuthenticationContext authContext = mock(XmAuthenticationContext.class);

        // Thread context
        ScopedContext threadContext = mock(ScopedContext.class);
        Mockito.when(threadContext.getValue(eq(THREAD_CONTEXT_KEY_TENANT_CONTEXT),
                                            eq(TenantContext.class))).thenReturn(tenantContext);

        // Execution context
        ScopedContext executionContext = Mockito.mock(ScopedContext.class);
        Map<String, Object> executionContextValues = new HashMap<>();
        executionContextValues.put(XmLepScriptConstants.BINDING_KEY_TENANT_CONTEXT, tenantContext);
        executionContextValues.put(XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT, authContext);
        Mockito.when(executionContext.getValues()).thenReturn(executionContextValues);

        ContextsHolder holder = Mockito.mock(ContextsHolder.class);
        Mockito.when(holder.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);
        Mockito.when(holder.getContext(eq(ContextScopes.EXECUTION))).thenReturn(executionContext);

        XmLepResourceService resourceService = new XmLepResourceService("test-app", CLASSPATH, resourceLoader);

        LepManagerService managerService = Mockito.mock(LepManagerService.class);
        Mockito.when(managerService.getResourceService()).thenReturn(resourceService);
        Mockito.when(managerService.getContext(eq(ContextScopes.THREAD))).thenReturn(threadContext);
        Mockito.when(managerService.getContext(eq(ContextScopes.EXECUTION))).thenReturn(executionContext);
        return managerService;
    }

    private static void switchTenantContext(LepManagerService managerService, String tenantKey) {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenantKey)));

        ScopedContext threadContext = managerService.getContext(ContextScopes.THREAD);

        when(threadContext.getValue(eq(THREAD_CONTEXT_KEY_TENANT_CONTEXT),
                                    eq(TenantContext.class))).thenReturn(tenantContext);

        ScopedContext executionContext = managerService.getContext(ContextScopes.EXECUTION);
        executionContext.getValues().put(XmLepScriptConstants.BINDING_KEY_TENANT_CONTEXT, tenantContext);
    }

    private LepMethod buildEmptyLepMethod() {
        MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
        Mockito.when(methodSignature.getParameterNames()).thenReturn(new String[0]);
        Mockito.when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[0]);

        LepMethod lepMethod = Mockito.mock(LepMethod.class);
        Mockito.when(lepMethod.getMethodArgValues()).thenReturn(new Object[0]);
        Mockito.when(lepMethod.getMethodSignature()).thenReturn(methodSignature);
        return lepMethod;
    }

    @Test
    public void simpleDefaultScriptValidExecution() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/Script.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("Script.groovy default", result);
    }

    @Test
    public void simpleTenantScriptValidExecution() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithTenant.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithTenant.groovy tenant, tenant: super", result);
    }

    @Test
    public void simpleAroundScriptValidExecution() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithAround.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithAround.groovy around, tenant: super", result);
    }

    @Test
    public void simpleBeforeAfterScriptValidExecution() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithBeforeAfter.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithBeforeAfter.groovy default", result);
    }

    @Test
    public void checkDefaultScriptBindingParams() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
        Mockito.when(methodSignature.getParameterNames()).thenReturn(new String[]{"name", "age"});

        LepMethod lepMethod = Mockito.mock(LepMethod.class);
        Mockito.when(lepMethod.getMethodArgValues()).thenReturn(new Object[]{"John Doe", 23});
        Mockito.when(lepMethod.getMethodSignature()).thenReturn(methodSignature);

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/CheckBindingParams.groovy");

        // execute script
        execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                        resourceExecutorSupplier);
    }

    /**
     * Demonstrate fixed issue #13555 - LEP script executed from other tenant.
     */
    @Test
    public void simpleAroundScriptSwitchTenantDespiteCompositeKey() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("/general/ScriptWithAround.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithAround.groovy around, tenant: super", result);

        switchTenantContext(managerService, "test");

        result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                 resourceExecutorSupplier);
        assertNotNull(result);
        // That was a critical issue after tenant contex swith script still executed from previous tenant!!!
        assertEquals("ScriptWithAround.groovy around, tenant: test", result);

    }

    @Test
    public void simpleAroundScriptSwitchTenantExecutionFixed() throws LepInvocationCauseException {
        LepManagerService managerService = buildLepManagerService("super");

        LepMethod lepMethod = buildEmptyLepMethod();

        UrlLepResourceKey compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("//super/general/ScriptWithAround.groovy");

        // execute script
        Object result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                        resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithAround.groovy around, tenant: super", result);

        switchTenantContext(managerService, "test");

        compositeKey = UrlLepResourceKey
            .valueOfUrlResourcePath("//test/general/ScriptWithAround.groovy");

        result = execStrategy.executeLepResource(compositeKey, lepMethod, managerService,
                                                 resourceExecutorSupplier);
        assertNotNull(result);
        assertEquals("ScriptWithAround.groovy around, tenant: test", result);

    }

    @Test
    public void testTenantContextSwitch() {

        LepManagerService managerService = buildLepManagerService("super");

        assertEquals("super", extractThreadTenantValue(managerService));
        assertEquals("super", extractExecutionTenantValue(managerService));

        switchTenantContext(managerService, "test");

        assertEquals("test", extractThreadTenantValue(managerService));
        assertEquals("test", extractExecutionTenantValue(managerService));

    }

    @Test
    public void testURLAuthority() throws MalformedURLException {

        UrlLepResourceKey urlLepResourceKey = new UrlLepResourceKey("lep://super/general/ScriptWithAround.groovy");
        assertEquals("super", urlLepResourceKey.getUrl().getAuthority());
        assertEquals("/general/ScriptWithAround.groovy", urlLepResourceKey.getUrl().getPath());
        assertEquals("lep", urlLepResourceKey.getUrl().getProtocol());

    }

    @Test
    public void testGetAvailableAtomicResourceKeys() throws MalformedURLException {

        LepManagerService managerService = buildLepManagerService("super");
        UrlLepResourceKey urlLepResourceKey = new UrlLepResourceKey("lep://super/general/ScriptWithAround.groovy");

        Map<XmLepResourceSubType, UrlLepResourceKey> availableAtomicResourceKeys = execStrategy
            .getAvailableAtomicResourceKeys(urlLepResourceKey, managerService);

        assertEquals(2, availableAtomicResourceKeys.size());

        assertEquals("lep://super/general/ScriptWithAround$$around.groovy",
                     availableAtomicResourceKeys.get(XmLepResourceSubType.AROUND).getId());
        assertEquals("lep://super/general/ScriptWithAround$$default.groovy",
                     availableAtomicResourceKeys.get(XmLepResourceSubType.DEFAULT).getId());


    }

    @Test
    public void testGetAvailableAtomicResourceKeysToNenantScript() throws MalformedURLException {

        LepManagerService managerService = buildLepManagerService("unknown");
        UrlLepResourceKey urlLepResourceKey = new UrlLepResourceKey("lep://unknown/general/ScriptWithAround.groovy");

        Map<XmLepResourceSubType, UrlLepResourceKey> availableAtomicResourceKeys = execStrategy
            .getAvailableAtomicResourceKeys(urlLepResourceKey, managerService);

        assertEquals(1, availableAtomicResourceKeys.size());
        assertTrue(availableAtomicResourceKeys.containsKey(XmLepResourceSubType.DEFAULT));

        assertEquals("lep://unknown/general/ScriptWithAround$$default.groovy",
                     availableAtomicResourceKeys.get(XmLepResourceSubType.DEFAULT).getId());

    }

    @Test
    public void testGetAvailableAtomicResourceKeysSwitchTenant() throws MalformedURLException {

        LepManagerService managerService = buildLepManagerService("super");
        UrlLepResourceKey urlLepResourceKey = new UrlLepResourceKey("lep://super/general/ScriptWithAround.groovy");

        Map<XmLepResourceSubType, UrlLepResourceKey> availableAtomicResourceKeys = execStrategy
            .getAvailableAtomicResourceKeys(urlLepResourceKey, managerService);

        assertEquals(2, availableAtomicResourceKeys.size());

        assertEquals("lep://super/general/ScriptWithAround$$around.groovy",
                     availableAtomicResourceKeys.get(XmLepResourceSubType.AROUND).getId());
        assertEquals("lep://super/general/ScriptWithAround$$default.groovy",
                     availableAtomicResourceKeys.get(XmLepResourceSubType.DEFAULT).getId());

        switchTenantContext(managerService, "test");

        urlLepResourceKey = new UrlLepResourceKey("lep://test/general/ScriptWithAround.groovy");

        availableAtomicResourceKeys = execStrategy
            .getAvailableAtomicResourceKeys(urlLepResourceKey, managerService);

        assertEquals("lep://test/general/ScriptWithAround$$around.groovy",
                     availableAtomicResourceKeys.get(XmLepResourceSubType.AROUND).getId());
        assertEquals("lep://test/general/ScriptWithAround$$default.groovy",
                     availableAtomicResourceKeys.get(XmLepResourceSubType.DEFAULT).getId());

    }

    private static String extractThreadTenantValue(final LepManagerService managerService) {
        TenantContext tenantContext = managerService
            .getContext(ContextScopes.THREAD)
            .getValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, TenantContext.class);
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContext);
    }

    private static String extractExecutionTenantValue(final LepManagerService managerService) {
        TenantContext tenantContext = (TenantContext) managerService
            .getContext(ContextScopes.EXECUTION)
            .getValues().get(XmLepScriptConstants.BINDING_KEY_TENANT_CONTEXT);
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContext);
    }

}

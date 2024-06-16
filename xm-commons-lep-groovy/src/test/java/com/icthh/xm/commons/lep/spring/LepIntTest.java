package com.icthh.xm.commons.lep.spring;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.lep.groovy.annotation.LepIgnoreInject;
import com.icthh.xm.commons.lep.groovy.annotation.LepInject;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.lep.spring.DynamicLepClassResolveIntTest.loadFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    DynamicLepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@ActiveProfiles("resolveclasstest")
public class LepIntTest {

    @Autowired
    private LepManagementService lepManagerService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private DynamicTestLepService testLepService;

    @Autowired
    private XmLepScriptConfigServerResourceLoader resourceLoader;

    @Before
    public void init() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagerService.beginThreadContext();
    }

    @Test
    @SneakyThrows
    public void testBackwardCompatibilityOfCallStaticMethodInChildClass() {
        String code = loadFile("lep/TestCallStaticMethodFromChildClass.groovy");
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithInput$$around.groovy", code);
        String result = testLepService.testLepMethod(Map.of());
        assertEquals("STATIC_METHOD_WORKS", result);
    }


    @Test
    @SneakyThrows
    public void testLepContextCastToMap() {
        String code = loadFile("lep/TestLepContextCastToMap.groovy");
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepMethodWithInput$$around.groovy", code);
        String result = testLepService.testLepMethod(Map.of("parameter", "testValue"));
        assertEquals("testValue", result);
    }

    @Test
    @SneakyThrows
    public void testLepConstructorAnnotation() {
        List<String> commonsClasses = List.of(
            "LepServiceAnnotatedAsInjectable",
            "LepServiceAnnotatedWithLepConstructor",
            "LepServiceWithExistingConstructor",
            "LepServiceWithExistingMapConstructor",
            "LepServiceWithExistingNoArgConstructor",
            "LepServiceInjectableNoArgConstructor",
            "LepServiceWithoutAnnotation"
        );
        commonsClasses.forEach(it -> {
            String lepCode = loadFile("lep/annotations/" + it + ".groovy");
            resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/commons/" + it + ".groovy", lepCode);
        });

        String testService = loadFile("lep/annotations/TestLepService.groovy");
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestLepService.groovy", testService);
        String code = loadFile("lep/annotations/TestLep.groovy");
        resourceLoader.onRefresh("/config/tenants/TEST/testApp/lep/service/TestWithReturnMap.groovy", code);
        Map<String, Object> result = testLepService.testWithReturnMap();
        assertGenerationResult(result.get("createdByCallNew"));
        assertGenerationResult(result.get("createdByServiceFactory"));
    }

    private void assertGenerationResult(Object result) {
        @RequiredArgsConstructor
        abstract class AssertHelper {
            private final Object value;

            @SneakyThrows
            Object get(String field) {
                Field declaredField = value.getClass().getDeclaredField(field);
                declaredField.setAccessible(true);
                return declaredField.get(value);
            }
        }

        new AssertHelper(result) {{
            assertNotNull(get("lep"));
            assertNotNull(get("thread"));
            assertEquals("First", get("stringWithValueFirst"));
            assertEquals("Second", get("stringWithValueSecond"));
            assertEquals("First", get("mustBeFirstHere"));
            assertEquals("Second", get("mustBeSecondHere"));
            assertEquals("First", get("firstMeetString"));
            assertEquals("this value will not be overridden", get("initedManually"));
            assertNull(get("notInited"));
            assertEquals(5, get("notPresentInLepContextNoAnnotationNeeded"));
            assertEquals("value", get("staticFieldIgnoredByLepConstructor"));

            assertNull(get("lepServiceWithoutAnnotation"));

            assertNotNull(get("lepServiceAnnotatedAsInjectable"));
            new AssertHelper(get("lepServiceAnnotatedAsInjectable")) {{
                assertEquals("First", get("stringWithValueFirst"));
            }};

            assertNotNull(get("lepServiceAnnotatedWithLepConstructor"));
            new AssertHelper(get("lepServiceAnnotatedWithLepConstructor")) {{
                assertEquals("First", get("stringWithValueFirst"));
                assertNotNull(get("lepServiceAnnotatedAsInjectable"));
                new AssertHelper(get("lepServiceAnnotatedAsInjectable")) {{
                    assertEquals("First", get("stringWithValueFirst"));
                }};
                assertNotNull(get("lepServiceWithExistingNoArgConstructor"));
                new AssertHelper(get("lepServiceWithExistingNoArgConstructor")) {{
                    assertEquals("First", get("stringWithValueFirst"));
                    assertEquals("setted in constructor", get("someValue"));
                }};
                assertNotNull(get("lepServiceInjectableNoArgConstructor"));
                new AssertHelper(get("lepServiceInjectableNoArgConstructor")) {{
                    assertEquals("setted in constructor", get("someValue"));
                }};
            }};

            assertNotNull(get("lepServiceWithExistingConstructor"));
            new AssertHelper(get("lepServiceWithExistingConstructor")) {{
                assertEquals("First", get("stringWithValueFirst"));
                assertEquals("setted in constructor", get("someValue"));
            }};

            assertNotNull(get("lepServiceInjectableNoArgConstructor"));
            new AssertHelper(get("lepServiceInjectableNoArgConstructor")) {{
                assertEquals("setted in constructor", get("someValue"));
            }};

//            assertNotNull(get("lepServiceWithExistingMapConstructor"));
//            new AssertHelper(get("lepServiceWithExistingMapConstructor")) {{
//                // if this assert failed, check is annotation processor enabled
//                assertEquals("First", get("stringWithValueFirst"));
//                assertEquals("setted in constructor", get("someValue"));
//            }};

            assertNotNull(get("lepServiceWithExistingNoArgConstructor"));
            new AssertHelper(get("lepServiceWithExistingNoArgConstructor")) {{
                assertEquals("First", get("stringWithValueFirst"));
                assertEquals("setted in constructor", get("someValue"));
            }};
        }};
    }

}

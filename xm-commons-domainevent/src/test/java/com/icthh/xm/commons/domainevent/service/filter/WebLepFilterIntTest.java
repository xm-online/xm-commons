package com.icthh.xm.commons.domainevent.service.filter;

import com.icthh.xm.commons.domainevent.config.TestLepTestConfig;
import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.lep.api.LepManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    TestLepTestConfig.class,
    TenantContextConfiguration.class,
    XmAuthenticationContextConfiguration.class
})
@TestPropertySource(properties = {
    "spring.application.name=app-name"
})
public class WebLepFilterIntTest {

    private static final String TENANT = "test";

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private WebLepFilter webLepFilter;

    @MockBean
    private KafkaTemplateService kafkaTemplateService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        TenantContextUtils.setTenant(tenantContextHolder, TENANT);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void shouldProcessByLep() {
        addLep();
        DomainEvent domainEvent = DomainEvent.builder()
            .id(UUID.randomUUID())
            .aggregateType("true")
            .build();
        boolean result = webLepFilter.lepFiltering("keyName", domainEvent);
        Assert.assertTrue(result);
    }

    private void addLep() {
        String prefix = "/config/tenants/" + TENANT.toUpperCase() + "/app-name/lep/filter/";
        String key = prefix + "WebFilter$$keyName$$around.groovy";
        String body =  "return true";
        leps.onRefresh(key, body);
    }

}

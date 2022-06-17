package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.spring.ApplicationLepProcessingEvent;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.ContextScopes;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.lep.api.LepProcessingEvent;
import com.icthh.xm.lep.api.ScopedContext;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.commons.lep.SeparatorSegmentedLepKeyResolver.translateToLepConvention;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

@LepService(group = "service.factory")
@Order(LOWEST_PRECEDENCE)
public class LepServiceFactoryImpl implements LepServiceFactory, RefreshableConfiguration,
        ApplicationListener<ApplicationLepProcessingEvent> {

    private static final String TEMPLATE_NAME = "lep/GeneratedServiceFactory.groovy";
    private static final String FACTORY_PACKAGE_NAME = "/service/factory/";
    private static final String GENERATED_SERVICE_FACTORY_LEP_NAME = "GeneratedServiceFactory";
    private static final String LEP_SUFFIX = "$$around.groovy";
    private static final String LEP_SERVICES = "lepServices";

    private final XmLepScriptConfigServerResourceLoader resourceLoader;
    private final TenantContextHolder tenantContextHolder;
    private final String factoryTemplate = loadFactoryTemplate();
    private final String applicationName;
    private final LepManager lepManager;

    private final Map<String, Map<Class<?>, Object>> serviceInstances = new ConcurrentHashMap<>();

    @Setter(onMethod = @__(@Autowired))
    private LepServiceFactoryImpl self;

    public LepServiceFactoryImpl(ApplicationNameHolder applicationName,
                                 XmLepScriptConfigServerResourceLoader resourceLoader,
                                 TenantContextHolder tenantContextHolder,
                                 LepManager lepManager) {
        this.applicationName = applicationName.getAppName();
        this.resourceLoader = resourceLoader;
        this.tenantContextHolder = tenantContextHolder;
        this.lepManager = lepManager;
    }

    @Override
    public <T> T getInstance(Class<T> lepServiceClass) {
        String simpleClassName = lepServiceClass.getSimpleName();

        String tenantKey = tenantContextHolder.getTenantKey();
        Map<Class<?>, Object> tenantInstances = serviceInstances.computeIfAbsent(tenantKey, key -> new ConcurrentHashMap<>());
        return (T) tenantInstances.computeIfAbsent(lepServiceClass, key -> {
            createServiceFactory(simpleClassName, tenantKey);
            return self.createServiceByLepFactory(simpleClassName, lepServiceClass);
        });
    }

    private <T> void createServiceFactory(String simpleClassName, String tenantKey) {
        String tenantName = tenantKey.toUpperCase();
        String pathForFactoryLep = "/config/tenants/" + tenantName + "/" + applicationName + "/lep";
        String lepKey = translateToLepConvention(simpleClassName);
        String lepName = GENERATED_SERVICE_FACTORY_LEP_NAME + "$$" + lepKey + LEP_SUFFIX;
        resourceLoader.onRefresh(pathForFactoryLep + FACTORY_PACKAGE_NAME + lepName, factoryTemplate);
        // no refreshFinished to avoid clear classLoader (class have just loaded)
    }

    @LogicExtensionPoint(value = "ServiceFactory", resolver = LepServiceFactoryResolver.class)
    public <T> T createServiceByLepFactory(String serviceClassName, Class<T> type) {
        return self.createServiceByGeneratedLepFactory(serviceClassName, type);
    }

    @LogicExtensionPoint(value = GENERATED_SERVICE_FACTORY_LEP_NAME, resolver = LepServiceFactoryResolver.class)
    public <T> T createServiceByGeneratedLepFactory(String serviceClassName, Class<T> type) {
        // Exception will never happen
        throw new RuntimeException("Error with service factory generation " + serviceClassName);
    }

    @Override
    public void onApplicationEvent(ApplicationLepProcessingEvent event) {
        if (event.getLepProcessingEvent() instanceof LepProcessingEvent.BeforeExecutionEvent) {
            ScopedContext context = lepManager.getContext(ContextScopes.EXECUTION);
            context.setValue(LEP_SERVICES, this);
        }
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        serviceInstances.clear();
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return resourceLoader.isListeningConfiguration(updatedKey);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        // Do nothing
    }

    @SneakyThrows
    private String loadFactoryTemplate() {
        InputStream templateResource = this.getClass().getClassLoader().getResourceAsStream(TEMPLATE_NAME);
        return IOUtils.toString(templateResource, UTF_8);
    }

}

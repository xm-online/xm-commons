package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.lep.storage.TenantScriptPathResolver;
import com.icthh.xm.lep.api.ExtensionService;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepResourceService;
import com.icthh.xm.lep.api.ScopedContext;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import static com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader.XM_MS_CONFIG_URL_PREFIX;

public class WarmupLepManagerService implements LepManagerService {

        private final AtomicBoolean warmupInProgress = new AtomicBoolean(true);
        private final LepManagerService lepManagerService;
        private final XmLepResourceService warmUpResourceService;

        public WarmupLepManagerService(String appName,
                                       TenantScriptPathResolver tenantScriptPathResolver,
                                       LepManagerService lepManagerService,
                                       Map<String, XmLepScriptResource> leps) {
            this.lepManagerService = lepManagerService;
            this.warmUpResourceService = new XmLepResourceService(appName, tenantScriptPathResolver, new ResourceLoader() {
                @Override
                public Resource getResource(String location) {
                    String cfgPath = StringUtils.removeStart(location, XM_MS_CONFIG_URL_PREFIX);
                    return leps.getOrDefault(cfgPath, XmLepScriptResource.nonExist());
                }
                @Override
                public ClassLoader getClassLoader() {
                    return ClassUtils.getDefaultClassLoader();
                }
            });
        }

        @Override
        public ExtensionService getExtensionService() {
            return lepManagerService.getExtensionService();
        }

        @Override
        public LepResourceService getResourceService() {
            return warmupInProgress.get() ? warmUpResourceService : lepManagerService.getResourceService();
        }

        @Override
        public ScopedContext getContext(String scope) {
            return lepManagerService.getContext(scope);
        }

        public void warmupFinished() {
            warmupInProgress.set(false);
        }
    }

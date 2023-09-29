package com.icthh.xm.commons.lep.js;

import com.icthh.xm.commons.lep.ProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Slf4j
@RequiredArgsConstructor
public class JsLepEngine extends LepEngine {

    private final String tenant;
    private final String appName;
    private final Map<String, XmLepConfigFile> leps;
    private final LoggingWrapper loggingWrapper;

    @Override
    public boolean isExists(LepKey lepKey) {
        String lepPath = toLepPath(lepKey);
        boolean exists = leps.containsKey(lepPath);
        if (!exists) {
            log.debug("Lep in js engine by path {} not found.", lepPath);
        }
        return exists;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    @SneakyThrows
    public Object invoke(LepKey lepKey, ProceedingLep lepMethod, BaseLepContext lepContext) {
        String lepPath = toLepPath(lepKey);
        if (leps.containsKey(lepPath)) {
            String lepName = toLepName(lepKey);
            return loggingWrapper.doWithLogs(lepMethod, lepName, lepKey, () ->
                runJsCode(leps.get(lepPath), lepContext, lepName)
            );
        } else {
            return lepContext.lep.proceed();
        }
    }

    @SneakyThrows
    private Object runJsCode(XmLepConfigFile xmLepConfigFile, BaseLepContext lepContext, String lepName) {
        try (Context context = Context.enter()) {
            Scriptable scope = context.initStandardObjects();

            Object wrappedLepContext = Context.javaToJS(lepContext, scope);
            ScriptableObject.putProperty(scope, "lepContext", wrappedLepContext);

            String jsCode = IOUtils.toString(xmLepConfigFile.getContentStream().getInputStream(), UTF_8);
            return context.evaluateString(scope, jsCode, lepName, 1, null);
        }
    }

    private String toLepName(LepKey lepKey) {
        String lepName = lepKey.getBaseKey();
        List<String> segments = lepKey.getSegments();
        if (isNotEmpty(segments)) {
            lepName = lepName + "__" + StringUtils.join(segments, "__");
        }
        return lepName;
    }

    private String toLepPath(LepKey lepKey) {
        String lepPath = lepKey.getBaseKey();
        List<String> segments = lepKey.getSegments();
        if (StringUtils.isNotBlank(lepKey.getGroup())) {
            lepPath = lepKey.getGroup().replace(".", "/") + "/" + lepKey.getBaseKey();
        }
        if (isNotEmpty(segments)) {
            lepPath = lepPath + "__" + StringUtils.join(segments, "__");
        }
        lepPath = "/config/tenants/" + tenant + "/" + appName + "/lep/" + lepPath + ".js";
        return lepPath;
    }

}

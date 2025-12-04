package com.icthh.xm.commons.service;

import com.icthh.xm.commons.domain.DefaultFunctionResult;
import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.keyresolver.FunctionLepKeyResolver;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.service.exporter.ExportService;
import com.icthh.xm.commons.service.exporter.ExportServiceProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@IgnoreLogginAspect
@LepService(group = "function")
@RequiredArgsConstructor
public class FunctionExecutorWrapperImpl implements FunctionExecutorWrapper {

    private final ExportServiceProvider exportServiceProvider;

    @Override
    @LogicExtensionPoint(value = "FunctionWrapper", resolver = FunctionLepKeyResolver.class)
    public FunctionResult execute(String functionKey, String fileFormat, Map<String, Object> functionInput, HttpServletResponse response) {
        ExportService exportService = exportServiceProvider.getExportService(fileFormat);
        exportService.export(functionKey, fileFormat, functionInput, response);
        return new DefaultFunctionResult(null);
    }
}

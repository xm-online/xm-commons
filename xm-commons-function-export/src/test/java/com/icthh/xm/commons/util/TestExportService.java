package com.icthh.xm.commons.util;

import com.icthh.xm.commons.service.FunctionExecutorService;
import com.icthh.xm.commons.service.exporter.AbstractExportServiceImpl;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public class TestExportService extends AbstractExportServiceImpl<TestRow> {

    public TestExportService(FunctionExecutorService functionExecutorService) {
        super(functionExecutorService);
    }

    @Override
    public boolean supports(String fileFormat) {
        return false;
    }

    @Override
    public void export(String functionKey, String fileFormat, Map<String, Object> functionInput, HttpServletResponse response) {

    }
}

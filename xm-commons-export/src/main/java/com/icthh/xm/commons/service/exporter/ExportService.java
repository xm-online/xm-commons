package com.icthh.xm.commons.service.exporter;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface ExportService {

    boolean supports(String fileFormat);

    void export(String functionKey, String fileFormat, Map<String, Object> functionInput, HttpServletResponse response);
}

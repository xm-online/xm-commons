package com.icthh.xm.commons.service;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface FunctionExportServiceFacade {

    /**
     * Execute function to write exported data to OutputStream in selected format.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param fileFormat    file format to download(csv, xlsx, etc.)
     * @param functionInput function input context
     */
    void execute(String functionKey, String fileFormat, Map<String, Object> functionInput, HttpServletResponse response);
}

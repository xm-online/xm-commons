package com.icthh.xm.commons.service;

import com.icthh.xm.commons.web.rest.response.DataSchemaResponse;

import java.util.List;

/**
 * Interface for function specification operations
 */
public interface FunctionSpecService {

    List<DataSchemaResponse> getDataSpecSchemas();
}

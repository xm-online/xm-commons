package com.icthh.xm.commons.web.rest.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataSchemaResponse {

    private String key;
    private String schema;
    private String type; // ui used parameter
}

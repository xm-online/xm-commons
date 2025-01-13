package com.icthh.xm.commons.swagger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BodyContent {

    @JsonProperty("application/json")
    private SwaggerContent applicationJson;
}

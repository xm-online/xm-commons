package com.icthh.xm.commons.flow.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TriggerDto {
    private String typeKey;
    private Map<String, Object> parameters;
}

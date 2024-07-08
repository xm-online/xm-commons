package com.icthh.xm.commons.flow.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class Trigger {
    private String typeKey;
    private Map<String, Object> parameters;
}
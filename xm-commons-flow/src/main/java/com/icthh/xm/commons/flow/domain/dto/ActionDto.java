package com.icthh.xm.commons.flow.domain.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActionDto extends StepDto {
    private Boolean isIterable;
    private String iterableJsonPath;
    private Boolean skipIterableJsonPathError;
    private List<String> next;
}

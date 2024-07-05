package com.icthh.xm.commons.flow.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConditionDto extends StepDto {
    private List<String> nextOnConditionTrue;
    private List<String> nextOnConditionFalse;
}

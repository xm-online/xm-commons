package com.icthh.xm.commons.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionResultDto {

    private Object data;
    private long executionTime;
    private String rid;
}

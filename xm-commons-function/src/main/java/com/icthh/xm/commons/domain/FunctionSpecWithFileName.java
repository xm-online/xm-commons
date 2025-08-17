package com.icthh.xm.commons.domain;

import com.icthh.xm.commons.domain.spec.IFunctionSpec;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FunctionSpecWithFileName<S extends IFunctionSpec> {
    private S item;
    private String fileKey;
}

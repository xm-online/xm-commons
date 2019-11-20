package com.icthh.xm.commons.config.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "of")
public class TenantState {
    @Builder.Default
    private String name = null;
    @Builder.Default
    private String state = null;
}

package com.icthh.xm.commons.config.domain;

import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor
public class TenantState {
    private String name = null;
    private String state = null;
}

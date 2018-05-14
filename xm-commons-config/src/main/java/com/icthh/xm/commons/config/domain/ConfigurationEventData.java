package com.icthh.xm.commons.config.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ConfigurationEventData {
    private String path;
    private String commit;
}

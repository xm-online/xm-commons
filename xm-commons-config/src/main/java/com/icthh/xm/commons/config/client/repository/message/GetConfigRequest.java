package com.icthh.xm.commons.config.client.repository.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetConfigRequest {
    private String version;
    private Collection<String> paths;
}

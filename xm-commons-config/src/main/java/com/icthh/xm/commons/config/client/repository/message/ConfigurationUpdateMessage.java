package com.icthh.xm.commons.config.client.repository.message;

import com.icthh.xm.commons.config.domain.Configuration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationUpdateMessage {

    private String path;
    private String content;
    private String oldConfigHash;

    public ConfigurationUpdateMessage(Configuration configuration, String oldConfigHash) {
        this.path = configuration.getPath();
        this.content = configuration.getContent();
        this.oldConfigHash = oldConfigHash;
    }
}

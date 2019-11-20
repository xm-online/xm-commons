package com.icthh.xm.commons.config.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "path")
@Data
@Builder(builderMethodName = "of")
public class Configuration {
    private String path;
    private String content;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Configuration{");
        sb.append("path='").append(path).append('\'');
        if (content != null && content.length() > 100) {
            sb.append(", content.length='").append(content.length()).append('\'');
        } else {
            sb.append(", content='").append(content).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}

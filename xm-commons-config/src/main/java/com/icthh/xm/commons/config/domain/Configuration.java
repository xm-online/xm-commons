package com.icthh.xm.commons.config.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "path")
@Data
public class Configuration {
    private String path;
    private String content;
    private String commit;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Configuration{");
        sb.append("path='").append(path).append('\'');
        sb.append(", commit='").append(commit).append('\'');
        if (content != null && content.length() > 100) {
            sb.append(", content.length='").append(content.length()).append('\'');
        } else {
            sb.append(", content='").append(content).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}

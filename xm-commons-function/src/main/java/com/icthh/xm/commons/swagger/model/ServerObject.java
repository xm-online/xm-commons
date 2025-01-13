package com.icthh.xm.commons.swagger.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServerObject {

    private String url;
    private String description;

    public ServerObject(String url) {
        this.url = url;
    }
}

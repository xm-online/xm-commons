package com.icthh.xm.commons.domainevent.domain.enums;

import lombok.Getter;

public enum DefaultDomainEventSource {
    DB("db"),
    WEB("web"),
    LEP("lep");

    @Getter
    private final String code;

    DefaultDomainEventSource(String code) {
        this.code = code;
    }
}

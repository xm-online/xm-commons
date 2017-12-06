package com.icthh.xm.commons.permission.domain;

public enum EnvironmentVariable {
    IP("ipAddress");

    private String name;

    EnvironmentVariable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

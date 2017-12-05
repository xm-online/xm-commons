package com.icthh.xm.commons.logging.web.rest.vm;

import ch.qos.logback.classic.Logger;
import lombok.NoArgsConstructor;

/**
 * View Model object for storing a Logback logger.
 */
@NoArgsConstructor
public class LoggerVm {

    private String name;

    private String level;

    public LoggerVm(Logger logger) {
        this.name = logger.getName();
        this.level = logger.getEffectiveLevel().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "LoggerVm{" +
            "name='" + name + '\'' +
            ", level='" + level + '\'' +
            '}';
    }

}

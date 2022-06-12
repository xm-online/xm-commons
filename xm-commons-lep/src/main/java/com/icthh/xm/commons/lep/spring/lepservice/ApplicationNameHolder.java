package com.icthh.xm.commons.lep.spring.lepservice;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * LepService it's annotation that declare class as Component.
 * That make impossible to declare it in configuration.
 * This class created for produce same value of variable appName by autowire to LepService from LepSpringConfiguration
 *
 */
@Getter
@AllArgsConstructor
public class ApplicationNameHolder {
    private final String appName;
}

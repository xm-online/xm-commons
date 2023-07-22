package com.icthh.xm.commons.lep.spring.lepservice;

public interface LepServiceFactory {

    <T> T getInstance(Class<T> lepServiceClass);

}

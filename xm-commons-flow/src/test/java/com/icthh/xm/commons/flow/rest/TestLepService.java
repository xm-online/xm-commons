package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;

@LepService(group = "test")
public class TestLepService {

    @LogicExtensionPoint("Test")
    public Object test() {
        return "test";
    }
}

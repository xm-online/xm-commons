package com.icthh.xm.commons.logging.configurable;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 *
 */
@Slf4j
@Service
@LepService(group = "general")
public class TestService {

    public String testMethodFirst(String firstArg, String secondArg) {
        return "result";
    }

    @LogicExtensionPoint("TestLep")
    public String testMethodSecond(String firstArg, String secondArg) {
        return "result";
    }

}

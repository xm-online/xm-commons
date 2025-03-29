package com.icthh.xm.commons.lep.spring;

import com.icthh.xm.commons.lep.TargetProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class TestLepContextCustomizer implements LepContextCustomizer {

    @Override
    public BaseLepContext customize(BaseLepContext lepContext, LepEngine lepEngine, TargetProceedingLep lepMethod) {
        log.info("customize lepContext: {}", lepContext.getClass().getCanonicalName());
        if (lepContext instanceof CustomTestLepContext custom) {
            CustomTestLepContext customTestLepContext = new CustomTestLepContext(custom);
            customTestLepContext.count = custom.count + 1;
            log.info("add count: {}", customTestLepContext.count);
            return customTestLepContext;
        }

        return new CustomTestLepContext(lepContext);

    }

    public static class CustomTestLepContext extends LepContext {

        public int count = 1;
        BaseLepContext originalLepContext;

        public CustomTestLepContext(BaseLepContext originalLepContext) {
            this.originalLepContext = originalLepContext;
            this.inArgs = originalLepContext.inArgs;
        }
    }

}

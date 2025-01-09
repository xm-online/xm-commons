package com.icthh.xm.commons.config;

import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.service.FunctionExecutorService;
import com.icthh.xm.commons.service.FunctionResultProcessor;
import com.icthh.xm.commons.service.FunctionService;
import com.icthh.xm.commons.service.FunctionServiceFacade;
import com.icthh.xm.commons.service.FunctionTxControl;
import com.icthh.xm.commons.service.impl.AbstractFunctionServiceFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FunctionApiConfiguration {

    @Bean
    @ConditionalOnMissingBean(FunctionResultProcessor.class)
    public FunctionResultProcessor<FunctionSpec> functionResultProcessor() {
        return (functionKey, executorResult, functionSpec) -> {
            log.debug("Function result wrapping is not implemented for function: {}", functionKey);
            return (FunctionResult) executorResult;
        };
    }

    @Bean
    @ConditionalOnMissingBean(FunctionServiceFacade.class)
    public FunctionServiceFacade functionServiceFacade(FunctionService<FunctionSpec> functionService,
                                                       FunctionTxControl functionTxControl,
                                                       FunctionExecutorService functionExecutorService,
                                                       FunctionResultProcessor<FunctionSpec> functionResultProcessor) {
        return new AbstractFunctionServiceFacade<>(functionService, functionTxControl, functionExecutorService, functionResultProcessor) {
        };
    }
}

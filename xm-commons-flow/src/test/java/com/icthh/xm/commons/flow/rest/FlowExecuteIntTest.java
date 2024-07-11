package com.icthh.xm.commons.flow.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.flow.domain.Action;
import com.icthh.xm.commons.flow.domain.Flow;
import com.icthh.xm.commons.flow.domain.Step;
import com.icthh.xm.commons.flow.domain.Trigger;
import com.icthh.xm.commons.flow.service.FlowConfigService;
import com.icthh.xm.commons.flow.spec.step.StepSpec;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import lombok.SneakyThrows;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.flow.steps.StepsRefreshableConfigurationUnitTest.loadFile;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FlowExecuteIntTest extends AbstractFlowIntTest {

    @Autowired
    TestLepService testLepService;

    @Autowired
    FlowConfigService flowConfigService;

    @Autowired
    XmLepScriptConfigServerResourceLoader lep;

    @Test
    public void test() {

    }
}

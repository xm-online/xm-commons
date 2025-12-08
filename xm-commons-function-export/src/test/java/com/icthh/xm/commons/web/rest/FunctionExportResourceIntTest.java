package com.icthh.xm.commons.web.rest;

import com.google.common.net.HttpHeaders;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.service.FunctionExportServiceFacade;
import com.icthh.xm.commons.service.FunctionServiceFacade;
import com.icthh.xm.commons.swagger.DynamicSwaggerFunctionGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest
@ContextConfiguration(classes = { // keep all controllers to test selected function API
    FunctionExportResource.class,
    FunctionResource.class,
    FunctionApiDocsResource.class,
    FunctionMvcResource.class,
    FunctionUploadResource.class,
    ExceptionTranslator.class
})
@TestPropertySource(properties = {
    "spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER"
})
class FunctionExportResourceIntTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @MockBean
    private FunctionServiceFacade functionService;

    @MockBean
    private FunctionExportServiceFacade functionExportServiceFacade;

    @MockBean
    private DynamicSwaggerFunctionGenerator dynamicSwaggerFunctionGenerator;

    @MockBean
    private LocalizationMessageService localizationMessageService;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void callExportFunction_shouldReturnOkAndCallService() throws Exception {
        mockMvc.perform(get("/api/export/functions/test")
                .param("fileName", "report")
                .param("fileFormat", "csv")
                .param("functionInput", "a:1")
            )
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.csv\""))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"));
    }

    @Test
    void callExportFunction_shouldMatchDotAndSlashInFunctionKey() throws Exception {
        mockMvc.perform(get("/api/export/functions/test/TEST")
                .param("fileName", "data")
                .param("fileFormat", "csv")
                .param("functionInput", "id:123"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data.csv\""))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"));
    }
}

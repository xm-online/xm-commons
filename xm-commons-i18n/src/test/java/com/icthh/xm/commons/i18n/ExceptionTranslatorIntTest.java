package com.icthh.xm.commons.i18n;

import static com.icthh.xm.commons.i18n.ExceptionTranslatorTestController.DEFAULT_MESSAGE;
import static com.icthh.xm.commons.i18n.ExceptionTranslatorTestController.MY_CUSTOM_MESSAGE;
import static com.icthh.xm.commons.i18n.I18nConstants.LANGUAGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.config.MessageSourceConfig;
import com.icthh.xm.commons.i18n.config.MockXmAuthenticationContextConfiguration;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.config.LocalizationMessageProperties;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import java.nio.charset.Charset;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test class for the ExceptionTranslator controller advice.
 *
 * @see ExceptionTranslator
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MessageSourceConfig.class,
        ExceptionTranslatorTestController.class, ExceptionTranslator.class,
        LocalizationMessageService.class, LocalizationMessageProperties.class,
        TenantContextConfiguration.class, MockXmAuthenticationContextConfiguration.class})
@WebAppConfiguration
public class ExceptionTranslatorIntTest {

    private static final String DEFAULT_TENANT_KEY = "TEST";
    private static final String DEFAULT_CONFIG_PATH = "config/tenants/" + DEFAULT_TENANT_KEY + "/i18n-message.yml";
    private static final String DEFAULT_CONFIG_KEY = "/" + DEFAULT_CONFIG_PATH;

    @Value("classpath:" + DEFAULT_CONFIG_PATH)
    private Resource configFile;

    @Autowired
    private ExceptionTranslatorTestController controller;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LocalizationMessageService localizationMessageService;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        XmAuthenticationContext authContext = Mockito.mock(XmAuthenticationContext.class);
        Mockito.when(authContextHolder.getContext()).thenReturn(authContext);
        Mockito.when(authContextHolder.getContext().getDetailsValue(LANGUAGE)).thenReturn(Optional.of("en"));


        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(exceptionTranslator)
            .build();
    }

    @Test
    public void testConcurrencyFailure() throws Exception {
        mockMvc.perform(get("/test/concurrency-failure"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_CONCURRENCY_FAILURE));
    }

    @Test
    public void testParameterizedError() throws Exception {
        mockMvc.perform(get("/test/parameterized-error"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business"))
            .andExpect(jsonPath("$.error_description").value("test parameterized error"))
            .andExpect(jsonPath("$.params.param0").value("param0_value"))
            .andExpect(jsonPath("$.params.param1").value("param1_value"));
    }

    @Test
    public void testParameterizedError2() throws Exception {
        mockMvc.perform(get("/test/parameterized-error2"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business"))
            .andExpect(jsonPath("$.error_description").value("test parameterized error"))
            .andExpect(jsonPath("$.params.foo").value("foo_value"))
            .andExpect(jsonPath("$.params.bar").value("bar_value"));
    }

    @Test
    public void testAccessDenied() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_ACCESS_DENIED))
            .andExpect(jsonPath("$.error_description").value("Access denied"));
    }

    @Test
    public void testMethodNotSupported() throws Exception {
        mockMvc.perform(post("/test/access-denied"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_METHOD_NOT_SUPPORTED))
            .andExpect(jsonPath("$.error_description").value("Method not supported"));
    }

    @Test
    public void testExceptionWithResponseStatus() throws Exception {
        mockMvc.perform(get("/test/response-status"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.400"))
            .andExpect(jsonPath("$.error_description").value("error.400"));
    }

    @Test
    public void testInternalServerError() throws Exception {
        mockMvc.perform(get("/test/internal-server-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_INTERNAL_SERVER_ERROR))
            .andExpect(jsonPath("$.error_description").value("Internal server error, please try later"));
    }

    @Test
    public void testFieldValidationError() throws Exception {
        mockMvc.perform(post("/test/field-validation-error")
            .content(new ObjectMapper().writeValueAsBytes(new ExceptionTranslatorTestController.TestFieldValidation()))
            .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_VALIDATION))
            .andExpect(jsonPath("$.error_description").value("Input parameters error"))
            .andExpect(jsonPath("$.fieldErrors.[0].objectName").value("testFieldValidation"))
            .andExpect(jsonPath("$.fieldErrors.[0].field").value("dummy"))
            .andExpect(jsonPath("$.fieldErrors.[0].message").value("NotNull"));
    }

    @Test
    public void testClassValidationError() throws Exception {
        mockMvc.perform(post("/test/class-validation-error")
            .content(new ObjectMapper().writeValueAsBytes(new ExceptionTranslatorTestController.TestFieldValidation()))
            .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_VALIDATION))
            .andExpect(jsonPath("$.error_description").value("Input parameters error"))
            .andExpect(jsonPath("$.fieldErrors.[0].objectName").value("testClassValidation"))
            .andExpect(jsonPath("$.fieldErrors.[0].field").value("testClassValidation"))
            .andExpect(jsonPath("$.fieldErrors.[0].message").value("NotCool"))
            .andExpect(jsonPath("$.fieldErrors.[0].description").isEmpty());
    }

    @Test
    public void testDefaultClassValidationError() throws Exception {
        mockMvc.perform(post("/test/default-message-class-validation-error")
                            .content(new ObjectMapper().writeValueAsBytes(new ExceptionTranslatorTestController.TestFieldValidation()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_VALIDATION))
               .andExpect(jsonPath("$.error_description").value("Input parameters error"))
               .andExpect(jsonPath("$.fieldErrors.[0].objectName").value("defaultMessageTestClassValidation"))
               .andExpect(jsonPath("$.fieldErrors.[0].field").value("defaultMessageTestClassValidation"))
               .andExpect(jsonPath("$.fieldErrors.[0].message").value("NotCool"))
               .andExpect(jsonPath("$.fieldErrors.[0].description").value(DEFAULT_MESSAGE));
    }

    @Test
    public void testCustomMessageTestClassValidation() throws Exception {
        mockMvc.perform(post("/test/custom-message-class-validation-error")
                            .content(new ObjectMapper().writeValueAsBytes(new ExceptionTranslatorTestController.TestFieldValidation()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_VALIDATION))
               .andExpect(jsonPath("$.error_description").value("Input parameters error"))
               .andExpect(jsonPath("$.fieldErrors.[0].objectName").value("customMessageTestClassValidation"))
               .andExpect(jsonPath("$.fieldErrors.[0].field").value("customMessageTestClassValidation"))
               .andExpect(jsonPath("$.fieldErrors.[0].message").value("NotCool"))
               .andExpect(jsonPath("$.fieldErrors.[0].description").value(MY_CUSTOM_MESSAGE));
    }

    @Test
    public void testBusinessErrorWithMessageFromConfig() throws Exception {
        //init config
        localizationMessageService.onInit(DEFAULT_CONFIG_KEY,
                        FileUtils.readFileToString(configFile.getFile(), Charset.forName("UTF-8")));

        mockMvc.perform(get("/test/message-from-config"))
            .andExpect(jsonPath("$.error").value("error.code"))
            .andExpect(jsonPath("$.error_description").value("Dummy error message"));
    }


    @Test
    public void testBusinessErrorWithTemplateMessageFromConfig() throws Exception {
        //init config
        localizationMessageService.onInit(DEFAULT_CONFIG_KEY,
            FileUtils.readFileToString(configFile.getFile(), Charset.forName("UTF-8")));

        mockMvc.perform(get("/test/template-message-from-config"))
                        .andExpect(jsonPath("$.error").value("error.code.with.placeholders"))
                        .andExpect(jsonPath("$.error_description").value(
                                        "My name is John Doe. I'm Java developer"));

        Mockito.when(authContextHolder.getContext().getDetailsValue(LANGUAGE)).thenReturn(
                        Optional.of("ru"));
        mockMvc.perform(get("/test/template-message-from-config"))
                        .andExpect(jsonPath("$.error").value("error.code.with.placeholders"))
                        .andExpect(jsonPath("$.error_description").value(
                                        "Меня зовут ${FULLName}. Я ${LANG} разработчик"));
    }
}

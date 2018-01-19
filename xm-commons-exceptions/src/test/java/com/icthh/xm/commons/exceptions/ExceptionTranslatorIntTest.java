package com.icthh.xm.commons.exceptions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.exceptions.spring.web.ExceptionTranslator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
@ContextConfiguration(classes = {MessageSourceConfig.class, ExceptionTranslatorTestController.class, ExceptionTranslator.class})
@WebAppConfiguration
public class ExceptionTranslatorIntTest {

    @Autowired
    private ExceptionTranslatorTestController controller;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    private MockMvc mockMvc;

    @Before
    public void setup() {
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
            .andExpect(jsonPath("$.fieldErrors.[0].message").value("NotCool"));
    }
}

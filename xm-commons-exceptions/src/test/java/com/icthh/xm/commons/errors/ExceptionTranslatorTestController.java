package com.icthh.xm.commons.errors;

import com.icthh.xm.commons.errors.exception.BusinessException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ExceptionTranslatorTestController {

    @GetMapping("/test/concurrency-failure")
    public void concurrencyFailure() {
        throw new ConcurrencyFailureException("test concurrency failure");
    }

    @GetMapping("/test/parameterized-error")
    public void parameterizedError() {
        throw new BusinessException("test parameterized error").withParams("param0_value", "param1_value");
    }

    @GetMapping("/test/parameterized-error2")
    public void parameterizedError2() {
        Map<String, String> params = new HashMap<>();
        params.put("foo", "foo_value");
        params.put("bar", "bar_value");
        throw new BusinessException("test parameterized error", params);
    }

    @GetMapping("/test/access-denied")
    public void accessdenied() {
        throw new AccessDeniedException("test access denied!");
    }

    @GetMapping("/test/response-status")
    public void exceptionWithReponseStatus() {
        throw new TestResponseStatusException();
    }

    @GetMapping("/test/internal-server-error")
    public void internalServerError() {
        throw new RuntimeException();
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "test response status")
    @SuppressWarnings("serial")
    public static class TestResponseStatusException extends RuntimeException {
    }

}

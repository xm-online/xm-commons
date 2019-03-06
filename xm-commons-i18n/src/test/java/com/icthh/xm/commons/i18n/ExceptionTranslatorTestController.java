package com.icthh.xm.commons.i18n;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.icthh.xm.commons.exceptions.BusinessException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
public class ExceptionTranslatorTestController {

    static final String DEFAULT_MESSAGE = "DEFAULT_MESSAGE";
    static final String MY_CUSTOM_MESSAGE = "MY_CUSTOM_MESSAGE";

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

    @PostMapping("/test/field-validation-error")
    public void fieldValidationError(@Valid @RequestBody TestFieldValidation dummy) {

    }

    @PostMapping("/test/class-validation-error")
    public void classValidationError(@Valid @RequestBody TestClassValidation dummy) {

    }

    @PostMapping("/test/default-message-class-validation-error")
    public void classValidationError(@Valid @RequestBody DefaultMessageTestClassValidation dummy) {

    }

    @PostMapping("/test/custom-message-class-validation-error")
    public void classValidationError(@Valid @RequestBody CustomMessageTestClassValidation dummy) {

    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "test response status")
    @SuppressWarnings("serial")
    public static class TestResponseStatusException extends RuntimeException {

    }

    public static class TestFieldValidation {

        @NotNull
        private String dummy;

        public TestFieldValidation() {

        }

        public String getDummy() {
            return dummy;
        }

        public void setDummy(String dummy) {
            this.dummy = dummy;
        }
    }

    @TestClassValidation.NotCool
    public static class TestClassValidation {

        @Target({TYPE})
        @Retention(RUNTIME)
        @Constraint(validatedBy = TestClassValidator.class)
        @Documented
        public @interface NotCool {

            String message() default "";

            Class<?>[] groups() default {};

            Class<? extends Payload>[] payload() default {};

        }

        public static class TestClassValidator implements ConstraintValidator<NotCool, Object> {

            public void initialize(NotCool constraintAnnotation) {
            }

            public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
                return false;
            }
        }
    }

    @CustomMessageTestClassValidation.NotCool
    public static class CustomMessageTestClassValidation {

        @Target({TYPE})
        @Retention(RUNTIME)
        @Constraint(validatedBy = TestClassValidator.class)
        @Documented
        public @interface NotCool {

            String message() default DEFAULT_MESSAGE;

            Class<?>[] groups() default {};

            Class<? extends Payload>[] payload() default {};

        }

        public static class TestClassValidator implements ConstraintValidator<NotCool, Object> {

            public void initialize(NotCool constraintAnnotation) {
            }

            public boolean isValid(Object value, ConstraintValidatorContext context) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(MY_CUSTOM_MESSAGE).addConstraintViolation();
                return false;
            }
        }
    }

    @DefaultMessageTestClassValidation.NotCool
    public static class DefaultMessageTestClassValidation {

        @Target({TYPE})
        @Retention(RUNTIME)
        @Constraint(validatedBy = TestClassValidator.class)
        @Documented
        public @interface NotCool {

            String message() default DEFAULT_MESSAGE;

            Class<?>[] groups() default {};

            Class<? extends Payload>[] payload() default {};

        }

        public static class TestClassValidator implements ConstraintValidator<NotCool, Object> {

            public void initialize(NotCool constraintAnnotation) {
            }

            public boolean isValid(Object value, ConstraintValidatorContext context) {
                return false;
            }
        }
    }

    @GetMapping("/test/message-from-config")
    public void businessErrorWithMessageFromConfig() {
        throw new BusinessException("error.code", "test message");
    }

    @GetMapping("/test/template-message-from-config")
    public void businessErrorWithTemplateMessageFromConfig() {
        Map<String, String> substitutes = new HashMap<>();
        substitutes.put("fullName", "John Doe");
        substitutes.put("language", "Java");
        throw new BusinessException("error.code.with.placeholders", "test message", substitutes);
    }
}

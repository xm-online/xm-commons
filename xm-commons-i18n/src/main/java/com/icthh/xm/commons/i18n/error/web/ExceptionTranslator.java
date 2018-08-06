package com.icthh.xm.commons.i18n.error.web;

import static com.icthh.xm.commons.i18n.I18nConstants.LANGUAGE;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.exceptions.NoContentException;
import com.icthh.xm.commons.exceptions.SkipPermissionException;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.i18n.error.domain.vm.FieldErrorVM;
import com.icthh.xm.commons.i18n.error.domain.vm.ParameterizedErrorVM;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Locale;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class ExceptionTranslator {

    private static final String ERROR_PREFIX = "error.";

    private final MessageSource messageSource;
    private final LocalizationMessageService localizationErrorMessageService;
    private final XmAuthenticationContextHolder authContextHolder;

    @ExceptionHandler(ConcurrencyFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorVM processConcurrencyError(ConcurrencyFailureException ex) {
        log.debug("Concurrency failure", ex);
        return new ErrorVM(ErrorConstants.ERR_CONCURRENCY_FAILURE, translate(ErrorConstants.ERR_CONCURRENCY_FAILURE));
    }

    @ExceptionHandler(SkipPermissionException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<Void> processSkipException(SkipPermissionException ex) {
        log.debug("Skip permission {}", ex.getPermission());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(NoContentException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public FieldErrorVM processConcurrencyError(NoContentException ex) {
        log.debug("No content", ex);
        return new FieldErrorVM(ErrorConstants.ERR_NOCONTENT, translate(ErrorConstants.ERR_NOCONTENT));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public FieldErrorVM processMissingServletRequestParameterError(MissingServletRequestParameterException ex) {
        FieldErrorVM dto = new FieldErrorVM(ErrorConstants.ERR_VALIDATION, translate(ErrorConstants.ERR_VALIDATION));
        dto.add(ex.getParameterType(), ex.getParameterName(), ex.getLocalizedMessage());
        return dto;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorVM processValidationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        FieldErrorVM dto = new FieldErrorVM(ErrorConstants.ERR_VALIDATION, translate(ErrorConstants.ERR_VALIDATION));
        for (FieldError fieldError : result.getFieldErrors()) {
            dto.add(fieldError.getObjectName(), fieldError.getField(), fieldError.getCode());
        }
        for (ObjectError globalError : result.getGlobalErrors()) {
            dto.add(globalError.getObjectName(), globalError.getObjectName(), globalError.getCode());
        }
        return dto;
    }

    @ExceptionHandler(HttpServerErrorException.class)
    @ResponseBody
    public ResponseEntity<ErrorVM> processHttpServerError(HttpServerErrorException ex) {
        BodyBuilder builder;
        ErrorVM fieldErrorVM;
        HttpStatus responseStatus = ex.getStatusCode();
        if (responseStatus != null) {
            builder = ResponseEntity.status(responseStatus.value());
            fieldErrorVM = new ErrorVM(ERROR_PREFIX + responseStatus.value(), translate(ERROR_PREFIX
                                                                                            + responseStatus.value()));
        } else {
            builder = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
            fieldErrorVM = new ErrorVM(ErrorConstants.ERR_INTERNAL_SERVER_ERROR,
                                       translate(ErrorConstants.ERR_INTERNAL_SERVER_ERROR));
        }
        return builder.body(fieldErrorVM);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ParameterizedErrorVM processParameterizedValidationError(BusinessException ex) {
        String code = ex.getCode() == null ? ErrorConstants.ERR_BUSINESS : ex.getCode();

        return new ParameterizedErrorVM(code, translate(code, false, ex.getMessage()),
                        ex.getParamMap());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorVM processNotFoundError(EntityNotFoundException ex) {
        log.debug("Entity not found", ex);
        return new ErrorVM(ErrorConstants.ERR_NOTFOUND, translate(ErrorConstants.ERR_NOTFOUND));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorVM processAccessDeniedException(AccessDeniedException e) {
        log.debug("Access denied", e);
        return new ErrorVM(ErrorConstants.ERR_ACCESS_DENIED, translate(ErrorConstants.ERR_ACCESS_DENIED));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorVM processMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        log.debug("Method not supported", exception);
        return new ErrorVM(ErrorConstants.ERR_METHOD_NOT_SUPPORTED, translate(ErrorConstants.ERR_METHOD_NOT_SUPPORTED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorVM> processException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        BodyBuilder builder;
        ErrorVM errorVM;
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            builder = ResponseEntity.status(responseStatus.value());
            errorVM = new ErrorVM(ERROR_PREFIX + responseStatus.value().value(),
                                  translate(ERROR_PREFIX + responseStatus.value().value()));
        } else {
            builder = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
            errorVM = new ErrorVM(ErrorConstants.ERR_INTERNAL_SERVER_ERROR,
                                  translate(ErrorConstants.ERR_INTERNAL_SERVER_ERROR));
        }
        return builder.body(errorVM);
    }

    /**
     * Method finds localization message by code and current locale from config. If not found it
     * takes message from message bundle or from default message first, depends on flag.
     * @param code the message code
     * @param firstFindInMessageBundle indicates where try to find message first when config has returned NULL
     * @param defaultMessage the default message
     * @return localized message
     */
    private String translate(String code, boolean firstFindInMessageBundle, String defaultMessage) {
        Locale locale = authContextHolder.getContext().getDetailsValue(LANGUAGE)
                        .map(Locale::forLanguageTag).orElse(LocaleContextHolder.getLocale());
        String translatedMessage = localizationErrorMessageService.getMessage(code, locale);
        if (translatedMessage == null) {
            if (firstFindInMessageBundle) {
                translatedMessage = messageSource.getMessage(code, null, locale);
            } else {
                translatedMessage = defaultMessage != null ? defaultMessage : messageSource
                                .getMessage(code, null, locale);
            }
        }

        return translatedMessage;
    }

    /**
     * Method finds localization message by code and current locale from config. If not found it
     * takes message from message bundle.
     * @param code the message code
     * @return localized message
     */
    private String translate(String code) {
        return translate(code, true, null);
    }
}

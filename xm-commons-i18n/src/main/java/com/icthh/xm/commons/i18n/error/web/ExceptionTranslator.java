package com.icthh.xm.commons.i18n.error.web;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.BusinessNotFoundException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.exceptions.NoContentException;
import com.icthh.xm.commons.exceptions.SkipPermissionException;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.i18n.error.domain.vm.FieldErrorVM;
import com.icthh.xm.commons.i18n.error.domain.vm.ParameterizedErrorVM;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_MESSAGE_NOT_READABLE;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class ExceptionTranslator {

    private static final String ERROR_PREFIX = "error.";

    private final LocalizationMessageService localizationErrorMessageService;

    @ExceptionHandler(ConcurrencyFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorVM processConcurrencyError(ConcurrencyFailureException ex) {
        log.debug("Concurrency failure", ex);
        return new ErrorVM(ErrorConstants.ERR_CONCURRENCY_FAILURE,
                        localizationErrorMessageService
                                        .getMessage(ErrorConstants.ERR_CONCURRENCY_FAILURE));
    }

    @ExceptionHandler(SkipPermissionException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<Void> processSkipException(SkipPermissionException ex) {
        log.debug("Skip permission {}", ex.getPermissions());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(NoContentException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public FieldErrorVM processConcurrencyError(NoContentException ex) {
        log.debug("No content", ex);
        return new FieldErrorVM(ErrorConstants.ERR_NOCONTENT,
                        localizationErrorMessageService.getMessage(ErrorConstants.ERR_NOCONTENT));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public FieldErrorVM processMissingServletRequestParameterError(MissingServletRequestParameterException ex) {
        FieldErrorVM dto = new FieldErrorVM(ErrorConstants.ERR_VALIDATION,
                        localizationErrorMessageService.getMessage(ErrorConstants.ERR_VALIDATION));
        dto.add(ex.getParameterType(), ex.getParameterName(), ex.getLocalizedMessage());
        return dto;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorVM processValidationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        FieldErrorVM dto = new FieldErrorVM(ErrorConstants.ERR_VALIDATION,
                        localizationErrorMessageService.getMessage(ErrorConstants.ERR_VALIDATION));
        for (FieldError fieldError : result.getFieldErrors()) {
            dto.add(fieldError.getObjectName(), fieldError.getField(), fieldError.getCode(),
                    fieldError.getDefaultMessage());
        }
        for (ObjectError globalError : result.getGlobalErrors()) {
            dto.add(globalError.getObjectName(), globalError.getObjectName(), globalError.getCode(),
                    globalError.getDefaultMessage());
        }
        return dto;
    }

    @ExceptionHandler(HttpServerErrorException.class)
    @ResponseBody
    public ResponseEntity<ErrorVM> processHttpServerError(HttpServerErrorException ex) {
        BodyBuilder builder;
        ErrorVM fieldErrorVM;
        // todo spring 3.2.0 migration
        HttpStatusCode responseStatus = ex.getStatusCode();
        builder = ResponseEntity.status(responseStatus.value());
        fieldErrorVM = new ErrorVM(ERROR_PREFIX + responseStatus.value(),
                        localizationErrorMessageService.getMessage(ERROR_PREFIX
                                        + responseStatus.value()));
        return builder.body(fieldErrorVM);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorVM processMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.debug("Message not readable", exception);
        return new ErrorVM(ErrorConstants.ERR_MESSAGE_NOT_READABLE,
            localizationErrorMessageService
                .getMessage(ErrorConstants.ERR_MESSAGE_NOT_READABLE));
    }

    @ExceptionHandler(BusinessNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ParameterizedErrorVM processBusinessNotFoundException(BusinessNotFoundException ex) {
        return createParameterizedErrorVM(ex);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ParameterizedErrorVM processParameterizedValidationError(BusinessException ex) {
      return createParameterizedErrorVM(ex);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorVM processNotFoundError(EntityNotFoundException ex) {
        log.debug("Entity not found", ex);
        return new ErrorVM(ErrorConstants.ERR_NOTFOUND,
                        localizationErrorMessageService.getMessage(ErrorConstants.ERR_NOTFOUND));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorVM processAccessDeniedException(AccessDeniedException e) {
        log.debug("Access denied", e);
        return new ErrorVM(ErrorConstants.ERR_ACCESS_DENIED,
                        localizationErrorMessageService
                                        .getMessage(ErrorConstants.ERR_ACCESS_DENIED));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorVM processMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        log.debug("Method not supported", exception);
        return new ErrorVM(ErrorConstants.ERR_METHOD_NOT_SUPPORTED,
                        localizationErrorMessageService
                                        .getMessage(ErrorConstants.ERR_METHOD_NOT_SUPPORTED));
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
                            localizationErrorMessageService.getMessage(ERROR_PREFIX
                                            + responseStatus.value().value()));
        } else {
            builder = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
            errorVM = new ErrorVM(ErrorConstants.ERR_INTERNAL_SERVER_ERROR,
                            localizationErrorMessageService
                                            .getMessage(ErrorConstants.ERR_INTERNAL_SERVER_ERROR));
        }
        return builder.body(errorVM);
    }

    private ParameterizedErrorVM createParameterizedErrorVM(BusinessException ex) {
        String code = ex.getCode() == null ? ErrorConstants.ERR_BUSINESS : ex.getCode();
        String message = localizationErrorMessageService.getMessage(code, ex.getParamMap(), false, ex.getMessage());
        return new ParameterizedErrorVM(code, message, ex.getParamMap());
    }
}

package com.icthh.xm.commons.utils;

import com.icthh.xm.commons.domain.FunctionResult;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.Optional;

import static com.icthh.xm.commons.utils.Constants.FUNCTION_CONTEXT;
import static com.icthh.xm.commons.utils.Constants.FUNCTION_CONTEXT_PATH;

@UtilityClass
public class ResponseEntityUtils {

    public static ResponseEntity<Object> processCreatedResponse(String appName, FunctionResult result) {
        return Optional.ofNullable(result.getId())
            .map(id -> URI.create(FUNCTION_CONTEXT_PATH + id))
            .map(uri -> buildCreatedResponseWithHeaders(appName, uri, result))
            .orElse(buildCreatedResponse(result));
    }

    private static ResponseEntity<Object> buildCreatedResponseWithHeaders(String appName, URI uri, FunctionResult result) {
        return ResponseEntity.created(uri)
            .headers(HeaderUtils.createEntityCreationAlert(appName, FUNCTION_CONTEXT, String.valueOf(result.getId())))
            .body(result.functionResult());
    }

    private static ResponseEntity<Object> buildCreatedResponse(FunctionResult result) {
        return ResponseEntity
            .status(HttpStatus.CREATED.value())
            .body(result.functionResult());
    }
}

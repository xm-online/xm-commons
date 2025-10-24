package com.icthh.xm.commons.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.swagger.DynamicSwaggerFunctionGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The {@link FunctionApiDocsResource} class.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FunctionApiDocsResource {

    private static final String X_DOMAIN = "x-domain";
    private static final String X_PORT = "x-port";
    public static final String PROTOCOL = "https://";

    private final DynamicSwaggerFunctionGenerator functionDocService;

    @Timed
    @GetMapping("/functions/api-docs")
    @PrivilegeDescription("Privilege to get openapi documentation for functions api")
    public ResponseEntity<Object> callGetFunction(HttpServletRequest request,
                                                  @RequestParam(value = "specName", required = false) String specName) {
        String url = PROTOCOL + request.getHeader(X_DOMAIN) + ":" + request.getHeader(X_PORT);
        return ResponseEntity.ok().body(functionDocService.generateSwagger(url, specName));
    }
}

package com.icthh.xm.commons.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.service.FunctionSpecService;
import com.icthh.xm.commons.service.impl.AbstractFunctionService;
import com.icthh.xm.commons.web.rest.response.DataSchemaResponse;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The {@link FunctionSpecResource} class.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class FunctionSpecResource {

    // prefix to avoid conflict with existing dynamic functions paths
    public static final String XME_SPEC_API = "/spec/functions";

    private final FunctionSpecService functionSpecService;
    private final AbstractFunctionService<?> functionService;

    @GetMapping(value = "/functions/dataschemas", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FUNCTION_SPEC.DATA_SCHEMA.GET')")
    @PrivilegeDescription("Privilege to get the function specification data schema")
    public ResponseEntity<List<DataSchemaResponse>> getDataSpecSchemas() {
        return ResponseEntity.ok().body(functionSpecService.getDataSpecSchemas());
    }

    @GetMapping(value = XME_SPEC_API + "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FUNCTION_SPEC.GET_LIST')")
    @PrivilegeDescription("Privilege to get the function specification data schema")
    public ResponseEntity<Collection<?>> getAllFunctions() {
        return ResponseEntity.ok().body(functionService.getAllFunctionSpecs());
    }

    @GetMapping(value = XME_SPEC_API + "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FUNCTION_SPEC.FILES.GET_LIST')")
    @PrivilegeDescription("Privilege to get the function specification data schema")
    public ResponseEntity<Collection<String>> getAllFiles() {
        return ResponseEntity.ok().body(functionService.getAllFileNames());
    }

}

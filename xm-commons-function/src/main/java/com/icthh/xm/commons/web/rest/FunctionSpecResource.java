package com.icthh.xm.commons.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.service.FunctionSpecService;
import com.icthh.xm.commons.web.rest.response.DataSchemaResponse;
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
@RequestMapping("/api/functions")
public class FunctionSpecResource {

    private final FunctionSpecService functionSpecService;

    @GetMapping(value = "/dataschemas", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FUNCTION_SPEC.DATA_SCHEMA.GET')")
    @PrivilegeDescription("Privilege to get the function specification data schema")
    public ResponseEntity<List<DataSchemaResponse>> getDataSpecSchemas() {
        return ResponseEntity.ok().body(functionSpecService.getDataSpecSchemas());
    }

    @GetMapping(value = "/function-spec", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FUNCTION_SPEC.SPEC.GET')")
    @PrivilegeDescription("Privilege to get the function list")
    public ResponseEntity<List<FunctionSpec>> getFunctionSpecList() {
        return ResponseEntity.ok().body(functionSpecService.getFunctionSpecList());
    }

}

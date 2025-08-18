package com.icthh.xm.commons.web.rest;

import static org.springframework.http.HttpStatus.CREATED;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.service.FunctionManageService;
import com.icthh.xm.commons.service.FunctionSpecService;
import com.icthh.xm.commons.service.impl.AbstractFunctionService;
import com.icthh.xm.commons.web.rest.response.DataSchemaResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final FunctionManageService<?, FunctionSpecWithFileName<?>> functionManageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @PostMapping(value = XME_SPEC_API, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PreAuthorize("hasPermission({'body': #body}, 'FUNCTION_SPEC.ADD')")
    @PrivilegeDescription("Privilege to add a function specification")
    public ResponseEntity<Void> addFunction(@RequestBody Map<String, Object> body) {
        var dto = readBody(body);
        functionManageService.addFunction(dto);
        return ResponseEntity.status(CREATED).build();
    }

    @PutMapping(value = XME_SPEC_API, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PreAuthorize("hasPermission({'body': #body}, 'FUNCTION_SPEC.UPDATE')")
    @PrivilegeDescription("Privilege to add a function specification")
    public ResponseEntity<Void> updateFunction(@RequestBody Map<String, Object> body) {
        var dto = readBody(body);
        functionManageService.updateFunction(dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = XME_SPEC_API + "/{functionKey}")
    @Timed
    @PreAuthorize("hasPermission({'fileKey': #fileKey, 'functionKey': #functionKey}, 'FUNCTION_SPEC.REMOVE')")
    @PrivilegeDescription("Privilege to remove a function specification")
    public ResponseEntity<Void> removeFunction(@PathVariable("functionKey") String functionKey) {
        functionManageService.removeFunction(functionKey);
        return ResponseEntity.ok().build();
    }

    @SneakyThrows
    private FunctionSpecWithFileName<?> readBody(Map<String, Object> body) {
        return objectMapper.convertValue(body, functionManageService.getFunctionSpecWrapperClass());
    }

}

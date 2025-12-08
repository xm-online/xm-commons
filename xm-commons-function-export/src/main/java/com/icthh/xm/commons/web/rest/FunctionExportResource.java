package com.icthh.xm.commons.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.service.FunctionExportServiceFacade;
import com.icthh.xm.commons.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.icthh.xm.commons.utils.HttpRequestUtils.getFunctionKey;

/**
 * The {@link FunctionExportResource} class.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FunctionExportResource {

    private final FunctionExportServiceFacade functionExportService;

    private FunctionExportResource self;

    @Autowired
    public void setSelf(@Lazy FunctionExportResource self) {
        this.self = self;
    }

    @Timed
    @GetMapping("/export/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.EXPORT.CALL')")
    @PrivilegeDescription("Privilege to execute an export function by key")
    public ResponseEntity<Void> callExportFunction(@PathVariable("functionKey") String functionKey,
                                                   @RequestParam String fileName,
                                                   @RequestParam String fileFormat,
                                                   @RequestParam(required = false) Map<String, Object> functionInput,
                                                   HttpServletResponse response) {

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "." + fileFormat + "\"");
        response.setContentType(HeaderUtil.getContentType(fileFormat));

        functionExportService.execute(functionKey, fileFormat, functionInput, response);

        return ResponseEntity.ok().build();
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/export/functions/**")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.EXPORT.CALL')")
    @PrivilegeDescription("Privilege to execute an export function by key")
    public ResponseEntity<Void> getCallExportFunction(HttpServletRequest request,
                                                   @RequestParam String fileName,
                                                   @RequestParam String fileFormat,
                                                   @RequestParam(required = false) Map<String, Object> functionInput,
                                                   HttpServletResponse response) {
        String functionKey = getFunctionKey(request);
        return self.callExportFunction(functionKey, fileName, fileFormat, functionInput, response);
    }
}

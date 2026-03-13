package com.icthh.xm.commons.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.service.FunctionServiceFacade;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.utils.HttpRequestUtils.getFunctionKey;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpMethod.POST;

/**
 * The {@link FunctionUploadResource} class.
 */
@Slf4j
@RestController
@RequestMapping("${application.functions-api-prefix:/api/functions}")
@RequiredArgsConstructor
public class FunctionUploadResource {

    public static final String UPLOAD = "/upload";
    public static final String HTTP_SERVLET_REQUEST_KEY = "httpServletRequest";
    public static final String FILES_KEY = "files";

    public static final String HTTP_INPUT_STREAM = "inputStream";
    public static final String FILENAME = "filename";
    public static final String UNKNOWN = "unknown";

    private final FunctionServiceFacade functionService;

    private FunctionUploadResource self;

    @Autowired
    public void setSelf(@Lazy FunctionUploadResource self) {
        this.self = self;
    }

    /**
     * See <a href="https://jevera.atlassian.net/wiki/spaces/XMED/pages/1788870657/F+Gate+-+File+Upload">File Upload</a>
     */
    @Deprecated(forRemoval = true)
    @Timed
    @PostMapping(value = "/**", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.UPLOAD.CALL')")
    @SneakyThrows
    @PrivilegeDescription("Privilege to call upload function")
    public ResponseEntity<Object> callUploadFunction(HttpServletRequest request,
                                                     @RequestParam(value = "file", required = false) List<MultipartFile> files,
                                                     HttpServletRequest httpServletRequest) {
        if (!request.getRequestURI().endsWith(UPLOAD)) {
            return ResponseEntity.badRequest().body("Invalid upload url");
        }
        Map<String, Object> functionInput = of(HTTP_SERVLET_REQUEST_KEY, httpServletRequest, FILES_KEY, files);
        String functionKey = getFunctionKey(request);
        functionKey = functionKey.substring(0, functionKey.length() - UPLOAD.length());
        FunctionResult result = functionService.execute(functionKey, functionInput, POST.name());
        return ResponseEntity.ok().body(result.functionResult());
    }

    @SneakyThrows
    @Timed
    @PostMapping("/upload/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.UPLOAD.CALL')")
    @PrivilegeDescription("Privilege to execute an upload function by key")
    public ResponseEntity<Object> callUploadFunction(@PathVariable("functionKey") String functionKey,
                                                     HttpServletRequest request,
                                                     @RequestParam("file") MultipartFile file) {
        String filename = ofNullable(file.getOriginalFilename()).orElse(UNKNOWN);

        try (InputStream inputStream = file.getInputStream()) {
            Map<String, Object> functionInput = of(
                HTTP_SERVLET_REQUEST_KEY, request,
                HTTP_INPUT_STREAM, inputStream,
                FILENAME, filename
            );
            FunctionResult result = functionService.execute(functionKey, functionInput, POST.name());
            return ResponseEntity.ok().body(result.functionResult());
        }
    }

    @SneakyThrows
    @PostMapping("/upload/functions/**")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.UPLOAD.CALL')")
    @PrivilegeDescription("Privilege to execute an upload function by key")
    public ResponseEntity<Object> getCallUploadFunction(HttpServletRequest request,
                                                        @RequestParam("file") MultipartFile file) {
        String functionKey = getFunctionKey(request);
        return self.callUploadFunction(functionKey, request, file);
    }
}

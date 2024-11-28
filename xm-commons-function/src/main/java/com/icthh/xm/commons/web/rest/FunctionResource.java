package com.icthh.xm.commons.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.service.FunctionServiceFacade;
import com.icthh.xm.commons.swagger.DynamicSwaggerFunctionGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.utils.Constants.POST_URLENCODED;
import static com.icthh.xm.commons.utils.Constants.UPLOAD;
import static com.icthh.xm.commons.utils.HttpRequestUtils.getFunctionKey;
import static com.icthh.xm.commons.utils.ModelAndViewUtils.getMvcResult;
import static com.icthh.xm.commons.utils.ResponseEntityUtils.processCreatedResponse;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * The {@link FunctionResource} class.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class FunctionResource {

    @Value("${spring.application.name}")
    private String applicationName;

    private final FunctionServiceFacade functionService;
    private final DynamicSwaggerFunctionGenerator functionDocService;

    private FunctionResource self;

    @Autowired
    public void setSelf(@Lazy FunctionResource self) {
        this.self = self;
    }

    @Timed
    @GetMapping("/functions/api-docs")
    @PrivilegeDescription("Privilege to get openapi documentation for functions api")
    public ResponseEntity<Object> callGetFunction(HttpServletRequest request) {
        String url = "https://" + request.getHeader("x-domain") + ":" + request.getHeader("x-port");
        return ResponseEntity.ok().body(functionDocService.generateSwagger(url));
    }

    @Timed
    @GetMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.GET.CALL')")
    @PrivilegeDescription("Privilege to call get function")
    public ResponseEntity<Object> callGetFunction(@PathVariable("functionKey") String functionKey,
                                                  @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.execute(functionKey, functionInput, GET.name());
        return ResponseEntity.ok().body(result.functionResult());
    }

    @Timed
    @PutMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.PUT.CALL')")
    @PrivilegeDescription("Privilege to call put function")
    public ResponseEntity<Object> callPutFunction(@PathVariable("functionKey") String functionKey,
                                                           @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.execute(functionKey, functionInput, PUT.name());
        return ResponseEntity.ok().body(result.functionResult());
    }

    @Timed
    @PatchMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.PATCH.CALL')")
    @PrivilegeDescription("Privilege to call patch function")
    public ResponseEntity<Object> callPatchFunction(@PathVariable("functionKey") String functionKey,
                                                    @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.execute(functionKey, functionInput, PATCH.name());
        return ResponseEntity.ok().body(result.functionResult());
    }

    /**
     * POST  /functions/{functionKey} : Execute a function by specification key.
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 201 (Created) and with body the new FunctionResult,
     * or with status 400 (Bad Request) if the FunctionResult has already an ID
     */
    @Timed
    @PostMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.CALL')")
    @PrivilegeDescription("Privilege to execute a function by specification key")
    public ResponseEntity<Object> callPostFunction(@PathVariable("functionKey") String functionKey,
                                                   @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.execute(functionKey, functionInput, POST.name());
        return processCreatedResponse(applicationName, result);
    }

    @Timed
    @PostMapping(value = "/functions/{functionKey:.+}", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.CALL')")
    @PrivilegeDescription("Privilege to execute a function by key (key in entity specification)")
    public ResponseEntity<Object> callPostFormFunction(@PathVariable("functionKey") String functionKey,
                                                       @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.execute(functionKey, functionInput, POST_URLENCODED);
        return processCreatedResponse(applicationName, result);
    }

    /**
     * DELETE  /functions/{functionKey} : Execute a function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 200 (Success) and with body the new FunctionResult,
     * or with status 400 (Bad Request) if the FunctionResult has already an ID
     */
    @Timed
    @DeleteMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.DELETE.CALL')")
    @PrivilegeDescription("Privilege to execute a function by key (key in entity specification)")
    public ResponseEntity<Object> callDeleteFunction(@PathVariable("functionKey") String functionKey,
                                                   @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.execute(functionKey, functionInput, DELETE.name());
        return ResponseEntity.ok().body(result.functionResult());
    }


    /**
     * POST  /functions/anonymous/{functionKey} : Execute an anonymous function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 201 (Created) and with body the new FunctionResult,
     * or with status 400 (Bad Request) if the FunctionResult has already an ID
     */
    @Timed
    @PostMapping("/functions/anonymous/{functionKey:.+}")
    public ResponseEntity<Object> callPostAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                            @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.executeAnonymous(functionKey, functionInput, POST.name());
        return processCreatedResponse(applicationName, result);
    }

    @Timed
    @PostMapping(value = "/functions/anonymous/{functionKey:.+}", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ResponseEntity<Object> callPostFormAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.executeAnonymous(functionKey, functionInput, POST_URLENCODED);
        return processCreatedResponse(applicationName, result);
    }


    /**
     * GET  /functions/anonymous/{functionKey} : Execute an anonymous function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 200 (Success) and with body the new FunctionResult,
     * or with status 400 (Bad Request) if the FunctionResult has already an ID
     */
    @Timed
    @GetMapping("/functions/anonymous/{functionKey:.+}")
    public ResponseEntity<Object> callGetAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                            @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.executeAnonymous(functionKey, functionInput, GET.name());
        return ResponseEntity.ok().body(result.functionResult());
    }

    /**
     * POST  /functions/mvc/{functionKey} : Execute a mvc function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return ModelAndView object
     */
    @Timed
    @PostMapping("/functions/mvc/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.MVC.CALL')")
    @PrivilegeDescription("Privilege to execute a mvc function by key (key in entity specification)")
    public ModelAndView callMvcFunction(@PathVariable("functionKey") String functionKey,
                                        @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.execute(functionKey, functionInput, POST.name());
        return getMvcResult(result);
    }

    @Timed
    @PostMapping(value = "/functions/mvc/{functionKey:.+}", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.MVC.CALL')")
    @PrivilegeDescription("Privilege to execute a mvc function by key (key in entity specification)")
    public ModelAndView callPostFormMvcFunction(@PathVariable("functionKey") String functionKey,
                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        return callMvcFunction(functionKey, functionInput);
    }

    @Timed
    @PostMapping("/functions/anonymous/mvc/{functionKey:.+}")
    public ModelAndView callMvcAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                 @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.executeAnonymous(functionKey, functionInput, POST.name());
        return getMvcResult(result);
    }

    @Timed
    @GetMapping("/functions/anonymous/mvc/{functionKey:.+}")
    public ModelAndView callMvcGetAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                 @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.executeAnonymous(functionKey, functionInput, GET.name());
        return getMvcResult(result);
    }

    @Timed
    @PostMapping(value = "/functions/anonymous/mvc/{functionKey:.+}", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ModelAndView callPostFormMvcAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                         @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionResult result = functionService.executeAnonymous(functionKey, functionInput, POST_URLENCODED);
        return getMvcResult(result);
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/**")
    public ResponseEntity<Object> callGetFunction(HttpServletRequest request,
                                  @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callGetFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/**")
    public ResponseEntity<Object> callPostFunction(HttpServletRequest request,
                                                   @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPostFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ResponseEntity<Object> callPostFormFunction(HttpServletRequest request,
                                                       @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callPostFormFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PutMapping("/functions/**")
    public ResponseEntity<Object> callPutFunction(HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPutFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PatchMapping("/functions/**")
    public ResponseEntity<Object> callPatchFunction(HttpServletRequest request,
                                                    @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPatchFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @DeleteMapping("/functions/**")
    public ResponseEntity<Object> callDeleteFunction(HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callDeleteFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/mvc/**")
    public ModelAndView callMvcFunction(HttpServletRequest request,
                                        @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callMvcFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/mvc/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ModelAndView callPostFormMvcFunction(HttpServletRequest request,
                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callMvcFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/anonymous/mvc/**")
    public ModelAndView callMvcAnonymousGetFunction(HttpServletRequest request,
                                                    @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callMvcGetAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/anonymous/mvc/**")
    public ModelAndView callMvcAnonymousFunction(HttpServletRequest request,
                                                 @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callMvcAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/anonymous/mvc/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ModelAndView callPostFromMvcAnonymousFunction(HttpServletRequest request,
                                                         @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callMvcAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/anonymous/**")
    public ResponseEntity<Object> callGetAnonymousFunction(HttpServletRequest request,
                                                  @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callGetAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/anonymous/**")
    public ResponseEntity<Object> callPostAnonymousFunction(HttpServletRequest request,
                                                            @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPostAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/anonymous/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ResponseEntity<Object> callPostFormAnonymousFunction(HttpServletRequest request,
                                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callPostFormAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @Timed
    @PostMapping(value = "/functions/**", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.UPLOAD.CALL')")
    @SneakyThrows
    @PrivilegeDescription("Privilege to call upload function")
    public ResponseEntity<Object> callUploadFunction(HttpServletRequest request,
                                                     @RequestParam(value = "file", required = false) List<MultipartFile> files,
                                                     HttpServletRequest httpServletRequest) {
        if (!request.getRequestURI().endsWith(UPLOAD)) {
            return ResponseEntity.badRequest().body("Invalid upload url");
        }
        Map<String, Object> functionInput = of("httpServletRequest", httpServletRequest, "files", files);
        String functionKey = getFunctionKey(request);
        functionKey = functionKey.substring(0, functionKey.length() - UPLOAD.length());
        FunctionResult result = functionService.execute(functionKey, functionInput, POST.name());
        return ResponseEntity.ok().body(result.functionResult());
    }
}

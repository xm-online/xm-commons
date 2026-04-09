package com.icthh.xm.commons.utils;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.FunctionResult;
import com.icthh.xm.commons.tenant.JsonMapperUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
@UtilityClass
public class ModelAndViewUtils {

    public static final String MVC_FUNC_RESULT = "modelAndView";
    private final ObjectMapper mapper = JsonMapperUtils.getDefaultJsonMapper();

    public static ModelAndView getMvcResult(FunctionResult result) {
        if (result == null) {
            return null;
        }
        ModelAndView modelAndView = result.getModelAndView();
        if (modelAndView == null) {
            Map<String, Object> dataMap = mapper.convertValue(result.getData(), new TypeReference<>() {});
            log.warn("Context did not contain {} or type (ModelAndView) mismatch, present keys {}", MVC_FUNC_RESULT, dataMap.keySet());
        }
        return modelAndView;
    }
}

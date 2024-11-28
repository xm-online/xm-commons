package com.icthh.xm.commons.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.util.AntPathMatcher;

import static com.icthh.xm.commons.utils.Constants.POST_URLENCODED;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

@UtilityClass
public class HttpRequestUtils {

    public static String getFunctionKey(HttpServletRequest request) {
        String path = request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        String bestMatchingPattern = request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        return new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
    }

    public static String convertToCanonicalHttpMethod(String httpMethod) {
        return POST_URLENCODED.equals(httpMethod) ? "POST" : httpMethod;
    }
}

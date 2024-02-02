package com.icthh.xm.commons.web.spring;

import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;

@UtilityClass
public class InterceptorUtil {

    public static void sendResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();
        writer.write(errorMessage);
        writer.flush();
    }
}

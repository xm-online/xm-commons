package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.utils.JsonMapperUtils;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public final class ForbiddenAccessDeniedHandler implements AccessDeniedHandler {
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        try (var writer = response.getWriter()) {
            writer.print(JsonMapperUtils.getDefaultJsonMapper().writeValueAsString(new ErrorVM(
                "forbidden",
                "Access is denied",
                MdcUtils.getRid()
            )));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ErrorVM {
        private final String error;
        private final String error_description;
        private final String requestId;
    }
}

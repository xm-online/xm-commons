package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.security.utils.JsonMapperUtils;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public final class ForbiddenAccessDeniedHandler implements AccessDeniedHandler {

    private static final String ERROR_CODE = "forbidden";

    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        try (var writer = response.getWriter()) {
            writer.print(JsonMapperUtils.getDefaultJsonMapper().writeValueAsString(new ErrorVM(
                ERROR_CODE,
                accessDeniedException.getMessage()
            )));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}

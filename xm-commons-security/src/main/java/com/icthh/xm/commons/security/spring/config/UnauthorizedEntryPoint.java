package com.icthh.xm.commons.security.spring.config;

import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.security.utils.JsonMapperUtils;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public final class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        try (var writer = response.getWriter()) {
            writer.print(JsonMapperUtils.getDefaultJsonMapper().writeValueAsString(new ErrorVM(
                ErrorConstants.ERR_UNAUTHORIZED,
                authException.getMessage()
            )));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}

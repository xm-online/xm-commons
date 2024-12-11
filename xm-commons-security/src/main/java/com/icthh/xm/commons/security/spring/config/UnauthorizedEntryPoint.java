package com.icthh.xm.commons.security.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public final class UnauthorizedEntryPoint implements AuthenticationEntryPoint {
	public void commence(HttpServletRequest request, HttpServletResponse response,
						 AuthenticationException authException) throws IOException {
		try (var writer = response.getWriter()) {
			writer.print(new ObjectMapper().writeValueAsString(new ErrorVM(
					"unauthorized",
					"Full authentication is required to access this resource"
			)));
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	@Getter
	@RequiredArgsConstructor
	public static class ErrorVM {
		private final String error;
		private final String error_description;
	}
}

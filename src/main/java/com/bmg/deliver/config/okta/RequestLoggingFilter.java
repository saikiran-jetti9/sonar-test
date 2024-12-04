package com.bmg.deliver.config.okta;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

import org.springframework.security.core.Authentication;

@Component
public class RequestLoggingFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String userId = getBGroupIdFromAuthentication();
		if (userId == null) {
			userId = "anonymous";
		}
		if (httpRequest.getRequestURI().contains("actuator")) {
			chain.doFilter(request, response);
			return;
		}
		logger.info("UserID: {}, Method: {}, URI: {}", userId, httpRequest.getMethod(), httpRequest.getRequestURI());
		chain.doFilter(request, response);
	}

	private String getBGroupIdFromAuthentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			Map<String, Object> profile = jwt.getClaim("profile");

			if (profile != null && profile.containsKey("BGroupId")) {
				return (String) profile.get("BGroupId");
			}
		}
		return null;
	}
}

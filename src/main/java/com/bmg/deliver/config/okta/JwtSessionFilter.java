package com.bmg.deliver.config.okta;

import com.bmg.deliver.serviceimpl.CustomUserInfoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Component
public class JwtSessionFilter extends GenericFilterBean {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String INTERNAL_TOKEN = "internal";
	private static final String JWT = "jwt";

	private final CustomUserInfoService customUserInfoService;

	public JwtSessionFilter(CustomUserInfoService customUserInfoService) {
		this.customUserInfoService = customUserInfoService;
	}
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		String token = extractToken(httpRequest);

		if (token != null) {
			if (INTERNAL_TOKEN.equalsIgnoreCase(token)) {
				setAuthenticationForInternalToken(token);
				httpRequest = new CustomHttpServletRequestWrapper(httpRequest, null);
			} else {
				httpRequest = new CustomHttpServletRequestWrapper(httpRequest, token);
			}
		}
		chain.doFilter(httpRequest, httpResponse);

	}

	private String extractToken(HttpServletRequest request) {
		// Get token from session
		String token = (String) request.getSession().getAttribute(JWT);

		// Get token from headers if not found in session
		if (token == null) {
			String authHeader = request.getHeader(AUTHORIZATION_HEADER);
			if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
				token = authHeader.substring(BEARER_PREFIX.length());
			}
		}
		return token;
	}

	private void setAuthenticationForInternalToken(String token) {
		Authentication authentication = customUserInfoService.loadAuthentication(token);
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);
	}

}

package com.bmg.deliver.config.okta;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
	private final String token;

	public CustomHttpServletRequestWrapper(HttpServletRequest request, String token) {
		super(request);
		this.token = token;
	}

	@Override
	public String getHeader(String name) {

		if (AUTHORIZATION_HEADER.equalsIgnoreCase(name) && token == null) {
			return null;
		}
		if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
			return BEARER_PREFIX + token;
		}
		return super.getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		List<String> headerNames = Collections.list(super.getHeaderNames());
		if (token == null) {
			return Collections.enumeration(
					headerNames.stream().filter(name -> !AUTHORIZATION_HEADER.equalsIgnoreCase(name)).toList());
		}
		if (!headerNames.contains(AUTHORIZATION_HEADER)) {
			headerNames.add(AUTHORIZATION_HEADER);
		}
		return Collections.enumeration(headerNames);
	}
}
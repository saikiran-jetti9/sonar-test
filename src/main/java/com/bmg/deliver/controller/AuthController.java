package com.bmg.deliver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class AuthController {

	@Value("${cors.allowedOrigins}")
	String appUrl;

	@Value("${frontend.path}")
	private String frontendPath;

	@GetMapping("/")
	public ResponseEntity<Object> loginRedirect() {
		return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).header("Location", appUrl + frontendPath).build();
	}

	@GetMapping("/logout")
	public ResponseEntity<Object> logout(HttpServletRequest request) {
		HashMap<String, String> responseMap = new HashMap<>();
		HttpSession session = request.getSession(false);
		SecurityContextHolder.clearContext();
		if (session != null) {
			session.invalidate();
		}
		responseMap.put("message", "Logout successful");
		return ResponseEntity.status(HttpStatus.OK).body(responseMap);
	}
}

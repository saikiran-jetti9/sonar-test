package com.bmg.deliver.controller;

import com.bmg.deliver.service.RemoteUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@InjectMocks
	private UserController userController;

	@Mock
	private RemoteUserService remoteUserService;

	@Mock
	private Authentication authentication;

	@Mock
	private SecurityContext securityContext;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testGetUser() {
		String mockPrincipal = "testUser";
		List<GrantedAuthority> mockAuthorities = new ArrayList<>();
		GrantedAuthority mockAuthority = mock(GrantedAuthority.class);
		mockAuthorities.add(mockAuthority);
		when(authentication.getPrincipal()).thenReturn(mockPrincipal);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		Map<String, Object> response = userController.getUser();
		assertEquals(mockPrincipal, response.get("principal"));
	}

	@Test
	void testGetBGroupIdWithValidBGroupId() {
		Jwt mockJwt = mock(Jwt.class);
		Map<String, Object> profile = new HashMap<>();
		profile.put("BGroupId", "testBGroupId");

		Map<String, Object> claims = new HashMap<>();
		claims.put("profile", profile);

		when(mockJwt.getClaims()).thenReturn(claims);
		String result = userController.getBGroupId(mockJwt);

		assertEquals("testBGroupId", result);
	}

	@Test
	void testGetBGroupIdWithNoProfile() {

		Jwt mockJwt = mock(Jwt.class);
		Map<String, Object> claims = new HashMap<>();

		when(mockJwt.getClaims()).thenReturn(claims);

		String result = userController.getBGroupId(mockJwt);

		assertEquals(null, result);
	}

	@Test
	void testGetBGroupIdWithNonJwtPrincipal() {
		Object nonJwtPrincipal = new Object();
		String result = userController.getBGroupId(nonJwtPrincipal);
		assertEquals(null, result);
	}

}

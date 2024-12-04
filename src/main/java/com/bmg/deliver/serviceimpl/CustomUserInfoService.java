package com.bmg.deliver.serviceimpl;

import com.bmg.deliver.utils.AppConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserInfoService {

	public Authentication loadAuthentication(String token) {

		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(AppConstants.ROLE_APP_CLIENT));
		Jwt jwt = Jwt.withTokenValue(token).headers(headers -> headers.put("alg", "none"))
				.claims(claims -> claims.put("sub", "internal")).build();
		return new JwtAuthenticationToken(jwt, authorities);
	}
}

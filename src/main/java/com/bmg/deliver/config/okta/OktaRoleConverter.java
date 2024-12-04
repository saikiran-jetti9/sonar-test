package com.bmg.deliver.config.okta;

import com.bmg.deliver.utils.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class OktaRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt source) {
		Map<String, Object> claims = source.getClaims();
		Object groupsObject = claims.get("groups");

		if (groupsObject instanceof List<?>) {
			List<String> groups = ((List<?>) groupsObject).stream().filter(String.class::isInstance)
					.map(String.class::cast).toList();

			List<GrantedAuthority> authorities = new ArrayList<>();
			authorities.add(new SimpleGrantedAuthority(AppConstants.ROLE_OKTA_USER));
			if (!groups.isEmpty()) {
				authorities.addAll(groups.stream().filter(group -> group.toLowerCase().contains("deliver"))
						.map(group -> new SimpleGrantedAuthority("ROLE_" + group)).toList());
			}

			return authorities;
		}
		return Collections.emptyList();
	}
}

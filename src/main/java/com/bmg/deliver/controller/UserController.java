package com.bmg.deliver.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmg.deliver.model.RemoteUser;
import com.bmg.deliver.service.RemoteUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.GrantedAuthority;

@RestController
@RequestMapping("/api/me")
public class UserController {

	@Autowired
	private RemoteUserService remoteUserService;

	@GetMapping
	public Map<String, Object> getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		Object principal = authentication.getPrincipal();
		List<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.toList();
		HashMap<String, Object> response = new HashMap<>();
		response.put("principal", principal);
		response.put("authorities", authorities);

		String bGroupId = getBGroupId(principal);
		handleUser(bGroupId);
		return response;
	}

	public String getBGroupId(Object principal) {
		if (principal instanceof Jwt jwt) {
			Map<String, Object> profile = (Map<String, Object>) jwt.getClaims().get("profile");

			if (profile != null && profile.containsKey("BGroupId")) {
				return (String) profile.get("BGroupId");
			}
		}
		return null;
	}

	private void handleUser(String bGroupId) {
		boolean userExists = checkIfUserExists(bGroupId);
		if (!userExists) {
			RemoteUser user = new RemoteUser();
			user.setUsername(bGroupId);
			remoteUserService.addUser(user);
		}
	}

	private boolean checkIfUserExists(String bGroupId) {
		List<RemoteUser> users = remoteUserService.getAllUsers();
		for (RemoteUser user : users) {
			if (bGroupId.equals(user.getUsername())) {
				return true;
			}
		}
		return false;
	}

}
package com.bmg.deliver.service;

import com.bmg.deliver.model.RemoteUser;

import java.util.List;
import java.util.Optional;

public interface RemoteUserService {
	RemoteUser addUser(RemoteUser remoteUser);

	List<RemoteUser> getAllUsers();

	Optional<RemoteUser> getUserByUsername(String username);
}

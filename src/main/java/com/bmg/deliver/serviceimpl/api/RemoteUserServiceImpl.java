package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.model.RemoteUser;
import com.bmg.deliver.repository.RemoteUserRepository;
import com.bmg.deliver.service.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RemoteUserServiceImpl implements RemoteUserService {

	@Autowired
	private RemoteUserRepository remoteUserRepository;

	@Override
	public RemoteUser addUser(RemoteUser remoteUser) {
		return remoteUserRepository.save(remoteUser);
	}

	@Override
	public List<RemoteUser> getAllUsers() {
		return remoteUserRepository.findAll();
	}

	@Override
	public Optional<RemoteUser> getUserByUsername(String username) {
		return remoteUserRepository.findByUsername(username);
	}
}

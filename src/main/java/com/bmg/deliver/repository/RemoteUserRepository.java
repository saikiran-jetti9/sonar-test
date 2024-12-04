package com.bmg.deliver.repository;

import com.bmg.deliver.model.RemoteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RemoteUserRepository extends JpaRepository<RemoteUser, Long> {
	Optional<RemoteUser> findByUsername(String username);
}

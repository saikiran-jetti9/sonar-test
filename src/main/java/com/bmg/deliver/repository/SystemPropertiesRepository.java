package com.bmg.deliver.repository;

import com.bmg.deliver.model.SystemProperties;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemPropertiesRepository extends JpaRepository<SystemProperties, Long> {
	Optional<SystemProperties> findByKey(String key);
}

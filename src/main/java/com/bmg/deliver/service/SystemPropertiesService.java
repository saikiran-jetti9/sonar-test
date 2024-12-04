package com.bmg.deliver.service;

import com.bmg.deliver.dto.SystemPropertiesDTO;
import com.bmg.deliver.model.SystemProperties;

public interface SystemPropertiesService {
	SystemProperties addSystemProperty(SystemPropertiesDTO systemPropertiesDTO);

	SystemProperties updateSystemProperty(Long id, SystemPropertiesDTO systemPropertiesDTO);

	SystemProperties getSystemPropertyByKey(String key);
}

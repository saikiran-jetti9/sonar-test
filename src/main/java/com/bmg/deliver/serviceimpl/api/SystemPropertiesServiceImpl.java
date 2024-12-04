package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.SystemPropertiesDTO;
import com.bmg.deliver.model.SystemProperties;
import com.bmg.deliver.repository.SystemPropertiesRepository;
import com.bmg.deliver.service.SystemPropertiesService;

import com.bmg.deliver.serviceimpl.master.MasterService;
import com.bmg.deliver.utils.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Conditional(MasterCondition.class)
@Service
public class SystemPropertiesServiceImpl implements SystemPropertiesService {

	private final SystemPropertiesRepository systemPropertiesRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final MasterService masterService;

	public SystemPropertiesServiceImpl(SystemPropertiesRepository systemPropertiesRepository,
			SimpMessagingTemplate messagingTemplate, MasterService masterService) {
		this.systemPropertiesRepository = systemPropertiesRepository;
		this.messagingTemplate = messagingTemplate;
		this.masterService = masterService;
	}

	@Override
	public SystemProperties addSystemProperty(SystemPropertiesDTO systemPropertiesDTO) {
		SystemProperties systemProperty = new SystemProperties();
		systemProperty.setKey(systemPropertiesDTO.getKey());
		systemProperty.setValue(systemPropertiesDTO.getValue());
		systemProperty.setDescription(systemPropertiesDTO.getDescription());
		return systemPropertiesRepository.save(systemProperty);
	}

	@Override
	public SystemProperties updateSystemProperty(Long id, SystemPropertiesDTO systemPropertiesDTO) {
		SystemProperties existingProperty = systemPropertiesRepository.findById(id).orElse(null);
		if (existingProperty == null) {
			return null;
		}
		existingProperty.setKey(systemPropertiesDTO.getKey());
		existingProperty.setValue(systemPropertiesDTO.getValue());
		existingProperty.setDescription(systemPropertiesDTO.getDescription());

		SystemProperties systemProperties = systemPropertiesRepository.save(existingProperty);
		if (AppConstants.PAUSED.equals(systemPropertiesDTO.getKey())) {
			boolean isPaused = Boolean.parseBoolean(systemPropertiesDTO.getValue());
			masterService.updatePausedProperty(isPaused);
			messagingTemplate.convertAndSend("/topic/paused-status", systemPropertiesDTO.getValue());

		}
		return systemProperties;
	}

	@Override
	public SystemProperties getSystemPropertyByKey(String key) {
		return systemPropertiesRepository.findByKey(key).orElse(null);
	}
}

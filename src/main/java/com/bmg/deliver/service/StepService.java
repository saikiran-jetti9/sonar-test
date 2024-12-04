package com.bmg.deliver.service;

import com.bmg.deliver.dto.WorkflowStepConfigurationDTO;
import com.bmg.deliver.dto.WorkflowStepDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface StepService {
	ResponseEntity<WorkflowStepDto> createStep(WorkflowStepDto workflowStepDto);

	ResponseEntity<WorkflowStepDto> updateStep(Long id, WorkflowStepDto workflowStepDto);

	void deleteStep(Long id);

	WorkflowStepDto createOrUpdateStepConfigs(Long id,
			List<WorkflowStepConfigurationDTO> workflowStepConfigurationDTOs);
}
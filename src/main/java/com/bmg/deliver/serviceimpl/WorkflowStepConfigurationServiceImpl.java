package com.bmg.deliver.serviceimpl;

import com.bmg.deliver.dto.WorkflowStepConfigurationDTO;
import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.exceptions.WorkflowNotFoundException;
import com.bmg.deliver.model.WorkflowStep;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.repository.WorkflowStepConfigurationRepository;
import com.bmg.deliver.repository.WorkflowStepRepository;
import com.bmg.deliver.service.WorkflowStepConfigurationService;
import com.bmg.deliver.utils.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowStepConfigurationServiceImpl implements WorkflowStepConfigurationService {

	@Autowired
	private WorkflowStepConfigurationRepository workflowStepConfigurationRepository;

	@Autowired
	private WorkflowStepRepository workflowStepRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	@Transactional
	public WorkflowStepConfiguration createWorkFlowStepConfiguration(Long stepId,
			WorkflowStepConfigurationDTO workflowStepConfigurationDTO) {
		try {
			WorkflowStep workflowStep = workflowStepRepository.findById(stepId)
					.orElseThrow(() -> new WorkflowNotFoundException(
							AppConstants.WORKFLOW_STEP_NOT_FOUND + workflowStepConfigurationDTO.getWorkflowStepId()));

			WorkflowStepConfiguration workflowStepConfiguration = new WorkflowStepConfiguration();
			workflowStepConfiguration.setKey(workflowStepConfigurationDTO.getKey());
			workflowStepConfiguration.setValue(workflowStepConfigurationDTO.getValue());
			workflowStepConfiguration.setWorkflowStep(workflowStep);

			return workflowStepConfigurationRepository.save(workflowStepConfiguration);
		} catch (WorkflowNotFoundException ex) {
			throw new WorkflowNotFoundException(AppConstants.ERROR_CREATING_WORKFLOW_STEP_CONFIGURATION);
		}
	}

	@Override
	public List<WorkflowStepConfiguration> getWorkflowStepConfigurationById(Long id) {
		try {
			List<WorkflowStepConfiguration> workflowStepConfigurations = workflowStepConfigurationRepository
					.findByWorkflowStepId(id);
			if (workflowStepConfigurations.isEmpty()) {
				return new ArrayList<>();
			}
			return workflowStepConfigurations;
		} catch (Exception e) {
			throw new WorkflowNotFoundException(AppConstants.ERROR_GETTING_WORKFLOW_STEP_CONFIGURATION);
		}
	}

	@Override
	public void deleteWorkflowStepConfigurationById(Long workflowStepConfigurationId) {
		try {
			workflowStepConfigurationRepository.deleteById(workflowStepConfigurationId);
		} catch (Exception e) {
			throw new WorkflowNotFoundException(AppConstants.ERROR_DELETING_WORKFLOW_STEP_CONFIGURATION);
		}
	}

	@Override
	public WorkflowStepConfiguration getWorkflowStepConfigurationByKey(String key) {
		try {
			WorkflowStepConfiguration workflowStepConfiguration = workflowStepConfigurationRepository.findByKey(key);
			if (workflowStepConfiguration == null) {
				throw new WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCES_NOT_FOUND + key);
			}
			return workflowStepConfiguration;
		} catch (Exception e) {
			throw new WorkflowNotFoundException(AppConstants.ERROR_GETTING_WORKFLOW_STEP_CONFIGURATION);
		}
	}

	@Override
	public List<WorkflowStepConfigurationDTO> getWorkflowStepConfigurationDtoByWorkflowStepId(Long id) {
		List<WorkflowStepConfiguration> workflowStepConfigurations = workflowStepConfigurationRepository
				.findByWorkflowStepId(id);
		List<WorkflowStepConfigurationDTO> listOfWorkflowStepConfigurationDTOs = new ArrayList<>();
		if (!workflowStepConfigurations.isEmpty()) {
			for (WorkflowStepConfiguration workflowStepConfiguration : workflowStepConfigurations) {
				WorkflowStepConfigurationDTO workflowStepConfigurationDTO = getWorkflowStepConfigurationDTO(
						workflowStepConfiguration);
				listOfWorkflowStepConfigurationDTOs.add(workflowStepConfigurationDTO);
			}
		} else {
			return listOfWorkflowStepConfigurationDTOs;
		}
		return listOfWorkflowStepConfigurationDTOs;

	}

	private WorkflowStepConfigurationDTO getWorkflowStepConfigurationDTO(
			WorkflowStepConfiguration workflowStepConfiguration) {
		WorkflowStepConfigurationDTO workflowStepConfigurationDTO = new WorkflowStepConfigurationDTO();
		workflowStepConfigurationDTO.setWorkflowStepId(workflowStepConfiguration.getWorkflowStep().getId());
		workflowStepConfigurationDTO.setId(workflowStepConfiguration.getId());
		workflowStepConfigurationDTO.setValue(workflowStepConfiguration.getValue());
		workflowStepConfigurationDTO.setKey(workflowStepConfiguration.getKey());
		return workflowStepConfigurationDTO;
	}

	@Override
	public WorkflowStepConfiguration updateWorkflowStepConfiguration(Long stepId,
			WorkflowStepConfigurationDTO workflowStepConfigurationDTO) {
		try {
			WorkflowStepConfiguration existingWorkflowStepConfiguration = workflowStepConfigurationRepository
					.findByWorkflowStepIdAndKey(stepId, workflowStepConfigurationDTO.getKey());
			if (existingWorkflowStepConfiguration == null) {
				return createWorkFlowStepConfiguration(stepId, workflowStepConfigurationDTO);
			} else {
				existingWorkflowStepConfiguration.setValue(workflowStepConfigurationDTO.getValue());
				return workflowStepConfigurationRepository.save(existingWorkflowStepConfiguration);
			}

		} catch (Exception e) {
			throw new WorkflowNotFoundException(AppConstants.ERROR_UPDATING_WORKFLOW_STEP_CONFIGURATION);
		}
	}
}

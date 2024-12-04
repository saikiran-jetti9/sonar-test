package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.*;
import com.bmg.deliver.exceptions.*;
import com.bmg.deliver.model.*;
import com.bmg.deliver.repository.*;
import com.bmg.deliver.service.StepService;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.service.WorkflowService;
import com.bmg.deliver.service.WorkflowStepConfigurationService;
import com.bmg.deliver.serviceimpl.master.MasterService;
import com.bmg.deliver.utils.AppConstants;
import com.nimbusds.jose.shaded.gson.JsonObject;
import jakarta.transaction.Transactional;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Conditional(MasterCondition.class)
@Service
public class WorkflowServiceImpl implements WorkflowService {
	private final WorkflowInstanceService workflowInstanceService;
	private final MasterService masterService;
	private final WorkflowRepository workflowRepository;

	private final WorkflowStepRepository workflowStepRepository;
	private final WorkflowInstanceRepository workflowInstanceRepository;
	private final WorkflowStepConfigurationService workflowStepConfigurationService;
	private final WorkflowConfigurationRepository workflowConfigurationRepository;
	private final StepService stepService;

	public WorkflowServiceImpl(WorkflowInstanceService workflowInstanceService, MasterService masterService,
			WorkflowRepository workflowRepository, WorkflowStepRepository workflowStepRepository,
			WorkflowInstanceRepository workflowInstanceRepository,
			WorkflowStepConfigurationService workflowStepConfigurationService,
			WorkflowConfigurationRepository workflowConfigurationRepository, StepService stepService) {
		this.workflowInstanceService = workflowInstanceService;
		this.masterService = masterService;
		this.workflowRepository = workflowRepository;
		this.workflowStepRepository = workflowStepRepository;
		this.workflowInstanceRepository = workflowInstanceRepository;
		this.workflowStepConfigurationService = workflowStepConfigurationService;
		this.workflowConfigurationRepository = workflowConfigurationRepository;
		this.stepService = stepService;
	}

	public WorkflowInstance createWorkFlowInstance(Long workflowId, JsonObject triggerData) {
		Workflow workflow = getWorkflowById(workflowId);
		if (!workflow.isEnabled()) {
			throw new WorkflowDisabledException(
					"Cannot create Instance as Workflow  " + workflow.getName() + " is disabled.");
		}
		WorkflowInstance workflowInstance = workflowInstanceService.createWorkFlowInstance(triggerData, workflow);

		// TODO FOR PAUSE
		WorkflowDTO workflowDTO = new WorkflowDTO();
		Workflow workflow1 = getWorkflowById(workflowId);
		workflowDTO.setEnabled(workflow1.isEnabled());
		workflowDTO.setPaused(workflow1.isPaused());
		// TO UPDATE INSTANCES BY WORKFLOW PAUSE
		workflowInstanceService.updateWorkflowInstanceStatus(workflowId, workflowDTO);

		masterService.processOnApi(workflowInstance);

		return workflowInstance;
	}

	@Override
	public WorkflowInstance updateWorkflowInstance(Long id, WorkflowInstanceDTO workflowInstanceDTO) {
		Optional<WorkflowInstance> workflowInstanceOptional = workflowInstanceRepository.findById(id);
		if (workflowInstanceOptional.isPresent()) {
			WorkflowInstance existingInstance = workflowInstanceOptional.get();
			if (workflowInstanceDTO.getStatus() != null) {
				existingInstance.setStatus(workflowInstanceDTO.getStatus());
			}
			if (workflowInstanceDTO.getPriority() != null) {
				existingInstance.setPriority(workflowInstanceDTO.getPriority());
				masterService.updateWorkflowInstance(existingInstance);
			}
			return workflowInstanceRepository.save(existingInstance);
		} else {
			throw new WorkflowInstanceIdNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id);
		}
	}

	@Override
	public Workflow getWorkflowById(Long id) {
		Optional<Workflow> optionalWorkflow = workflowRepository.findById(id);
		if (optionalWorkflow.isPresent()) {
			return optionalWorkflow.get();
		} else {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND);
		}
	}

	@Override
	public Page<WorkflowDTO> getAllWorkflows(String search, Pageable pageable, Boolean enabled, Date startDate,
			Date endDate) {
		Date[] dates = {startDate, endDate};
		setFilterDates(dates);
		startDate = dates[0];
		endDate = dates[1];

		Page<Workflow> workflows;
		if (search == null || search.isEmpty()) {
			workflows = findWorkflowsByFilters(enabled, startDate, endDate, pageable);
		} else {
			workflows = findWorkflowsBySearch(search, enabled, startDate, endDate, pageable);
		}

		return convertToWorkflowDTOs(workflows);
	}

	private Page<Workflow> findWorkflowsByFilters(Boolean enabled, Date startDate, Date endDate, Pageable pageable) {
		if (enabled != null && startDate != null && endDate != null) {
			return workflowRepository.findByEnabledAndCreatedBetween(enabled, startDate, endDate, pageable);
		} else if (enabled != null) {
			return workflowRepository.findByEnabled(enabled, pageable);
		} else if (startDate != null && endDate != null) {
			return workflowRepository.findByCreatedBetween(startDate, endDate, pageable);
		} else {
			return workflowRepository.findAll(pageable);
		}
	}

	private Page<Workflow> findWorkflowsBySearch(String search, Boolean enabled, Date startDate, Date endDate,
			Pageable pageable) {
		if (enabled != null && startDate != null && endDate != null) {
			return workflowRepository.findByNameContainingIgnoreCaseAndEnabledAndCreatedBetween(search, enabled,
					startDate, endDate, pageable);
		} else if (enabled != null) {
			return workflowRepository.findByNameContainingIgnoreCaseAndEnabled(search, enabled, pageable);
		} else if (startDate != null && endDate != null) {
			return workflowRepository.findByNameContainingIgnoreCaseAndCreatedBetween(search, startDate, endDate,
					pageable);
		} else {
			return workflowRepository.findByNameContainingIgnoreCase(search, pageable);
		}
	}

	/**
	 * Helper Method to convert Workflows to Dto
	 *
	 * @param workflows
	 */
	private Page<WorkflowDTO> convertToWorkflowDTOs(Page<Workflow> workflows) {
		return workflows.map(workflow -> {
			WorkflowDTO workflowDTO = new WorkflowDTO();
			workflowDTO.setId(workflow.getId());
			workflowDTO.setName(workflow.getName());
			workflowDTO.setDescription(workflow.getDescription());
			workflowDTO.setEnabled(workflow.isEnabled());
			workflowDTO.setThrottleLimit(workflow.getThrottleLimit());
			workflowDTO.setCreated(workflow.getCreated());
			workflowDTO.setModified(workflow.getModified());
			workflowDTO.setIsTaskChainIsValid(workflow.isTaskChainIsValid());
			workflowDTO.setAlias(workflow.getAlias());
			workflowDTO.setAssetIngestionTime(workflow.getAssetIngestionTime());
			workflowDTO.setDataIngestionTime(workflow.getDataIngestionTime());
			workflowDTO.setPaused(workflow.isPaused());

			Optional<WorkflowInstance> optionalInstance = workflowInstanceRepository
					.findTopByStatusAndWorkflowIdOrderByCompletedDesc(workflow.getId());
			optionalInstance.ifPresent(instance -> {
				workflowDTO.setStatus(instance.getStatus().toString());
				workflowDTO.setCompleted(instance.getCompleted());
			});
			return workflowDTO;
		});
	}

	/**
	 * Helper Method to Format Dates
	 *
	 * @param dates
	 */
	private void setFilterDates(Date[] dates) {

		if (dates[0] != null && dates[1] == null) {
			dates[1] = dates[0];
		}

		if (dates[0] != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dates[0]);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			dates[0] = cal.getTime();
		}

		if (dates[1] != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dates[1]);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			dates[1] = cal.getTime();
		}
	}

	@Override
	public List<WorkflowStepDto> getWorkflowSteps(Long id) {
		List<WorkflowStep> workflowSteps = workflowStepRepository.findByWorkflowIdOrderByExecutionOrder(id);

		return workflowSteps.stream().map(step -> {
			WorkflowStepDto workflowStepDto = new WorkflowStepDto();
			workflowStepDto.setId(step.getId());
			workflowStepDto.setWorkflowId(step.getWorkflow().getId());
			workflowStepDto.setExecutionOrder(step.getExecutionOrder());
			workflowStepDto.setType(step.getType());
			workflowStepDto.setName(step.getName());
			workflowStepDto.setCreated(step.getCreated());
			workflowStepDto.setModified(step.getModified());
			workflowStepDto.setWorkflowStepConfigurations(
					workflowStepConfigurationService.getWorkflowStepConfigurationDtoByWorkflowStepId(step.getId()));
			return workflowStepDto;
		}).toList();
	}

	@Override
	public Workflow createWorkflow(WorkflowDTO workflowDTO) {
		if (workflowDTO.getAlias() != null && !workflowDTO.getAlias().isEmpty()) {
			isAliasExist(workflowDTO.getAlias());
		}
		Workflow workflow = new Workflow();
		workflow.setName(workflowDTO.getName());
		workflow.setDescription(workflowDTO.getDescription());
		if (workflowDTO.getPaused() != null) {
			workflow.setPaused(workflowDTO.getPaused());
		}
		if (workflowDTO.getEnabled() != null) {
			workflow.setEnabled(workflowDTO.getEnabled());
		}
		if (workflowDTO.getIsTaskChainIsValid() != null) {
			workflow.setTaskChainIsValid(workflowDTO.getIsTaskChainIsValid());
		}
		if (workflowDTO.getAssetIngestionTime() != null && !workflowDTO.getAssetIngestionTime().isEmpty()) {
			workflow.setAssetIngestionTime(workflowDTO.getAssetIngestionTime());
		} else {
			workflow.setAssetIngestionTime(AppConstants.DEFAULT_VALUE);
		}

		if (workflowDTO.getDataIngestionTime() != null && !workflowDTO.getDataIngestionTime().isEmpty()) {
			workflow.setDataIngestionTime(workflowDTO.getDataIngestionTime());
		} else {
			workflow.setDataIngestionTime(AppConstants.DEFAULT_VALUE);
		}
		workflow.setThrottleLimit(workflowDTO.getThrottleLimit());
		workflow.setAlias(workflowDTO.getAlias());
		// To Update in Master
		Workflow workflow1 = workflowRepository.save(workflow);
		masterService.addWorkflow(workflow1);
		return workflow1;
	}

	public void isAliasExist(String alias) {
		Optional<Workflow> optionalWorkflow = workflowRepository.findByAlias(alias);
		if (optionalWorkflow.isPresent()) {
			log.info(AppConstants.WORKFLOW_ALIAS_ALREADY_EXISTS + alias);
			throw new WorkflowAliasExistsException(AppConstants.WORKFLOW_ALIAS_ALREADY_EXISTS + alias);
		}
	}

	@Override
	public Workflow updateWorkflow(Long id, WorkflowDTO workflowDTO) {
		try {
			Workflow existingWorkflow = getWorkflowById(id);
			if (workflowDTO.getName() != null) {
				existingWorkflow.setName(workflowDTO.getName());
			}

			if (workflowDTO.getDescription() != null) {
				existingWorkflow.setDescription(workflowDTO.getDescription());
			}

			if (workflowDTO.getThrottleLimit() != null) {
				existingWorkflow.setThrottleLimit(workflowDTO.getThrottleLimit());
			}
			if (workflowDTO.getAlias() != null) {
				if (!existingWorkflow.getAlias().equals(workflowDTO.getAlias())) {
					isAliasExist(workflowDTO.getAlias());
				}
				existingWorkflow.setAlias(workflowDTO.getAlias());
			}

			if (workflowDTO.getAssetIngestionTime() != null) {
				existingWorkflow.setAssetIngestionTime(workflowDTO.getAssetIngestionTime());
			}

			if (workflowDTO.getDataIngestionTime() != null) {
				existingWorkflow.setDataIngestionTime(workflowDTO.getDataIngestionTime());
			}
			if (workflowDTO.getEnabled() != null) {
				existingWorkflow.setEnabled(workflowDTO.getEnabled());
			}

			if (workflowDTO.getPaused() != null) {
				existingWorkflow.setPaused(workflowDTO.getPaused());
			}

			if (workflowDTO.getIsTaskChainIsValid() != null) {
				existingWorkflow.setTaskChainIsValid(workflowDTO.getIsTaskChainIsValid());
			}
			if (workflowDTO.getCreated() != null) {
				existingWorkflow.setCreated(workflowDTO.getCreated());
			}

			Workflow workflow = workflowRepository.save(existingWorkflow);
			// Updating the status of instances based on the pause
			workflowInstanceService.updateWorkflowInstanceStatus(id, workflowDTO);
			masterService.updateWorkflow(workflow);
			return workflow;
		} catch (WorkflowNotFoundException e) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND);
		}
	}

	@Override
	@Transactional
	public void deleteWorkflow(Long workflowId) {
		try {
			getWorkflowById(workflowId);
			List<WorkflowStep> workflowSteps = workflowStepRepository.findByWorkflowId(workflowId);
			for (WorkflowStep workflowStep : workflowSteps) {
				stepService.deleteStep(workflowStep.getId());
			}

			workflowStepRepository.deleteAllByWorkflowId(workflowId);
			workflowInstanceRepository.deleteAllByWorkflowId(workflowId);
			workflowConfigurationRepository.deleteAllByWorkflowId(workflowId);

			workflowRepository.deleteById(workflowId);
		} catch (WorkflowNotFoundException e) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + workflowId);
		}
	}

	@Override
	public Page<WorkflowDTO> getWorkflowsByWorkflowName(String identifier, Pageable pageable) {
		Page<Workflow> workflows = workflowRepository.findByName(identifier, pageable);

		return workflows.map(workflow -> {
			WorkflowDTO workflowDTO = new WorkflowDTO();
			workflowDTO.setId(workflow.getId());
			workflowDTO.setName(workflow.getName());
			workflowDTO.setDescription(workflow.getDescription());
			workflowDTO.setEnabled(workflow.isEnabled());
			workflowDTO.setThrottleLimit(workflow.getThrottleLimit());
			workflowDTO.setCreated(workflow.getCreated());
			workflowDTO.setModified(workflow.getModified());
			workflowDTO.setIsTaskChainIsValid(workflow.isTaskChainIsValid());
			workflowDTO.setPaused(workflow.isPaused());
			workflowDTO.setAlias(workflow.getAlias());

			Optional<WorkflowInstance> optionalInstance = workflowInstanceRepository
					.findTopByStatusAndWorkflowIdOrderByCompletedDesc(workflow.getId());
			optionalInstance.ifPresent(instance -> workflowDTO.setStatus(instance.getStatus().toString()));

			return workflowDTO;
		});
	}

	@Override
	public WorkflowConfiguration createWorkFlowConfiguration(Long workflowId,
			WorkflowConfigurationDTO workflowConfigurationDTO) {
		try {
			WorkflowConfiguration workflowConfiguration = new WorkflowConfiguration();
			workflowConfiguration.setKey(workflowConfigurationDTO.getKey());
			workflowConfiguration.setValue(workflowConfigurationDTO.getValue());
			workflowConfiguration.setWorkflow(getWorkflowById(workflowId));
			return workflowConfigurationRepository.save(workflowConfiguration);
		} catch (WorkflowNotFoundException ex) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + workflowId);
		}
	}

	@Override
	public WorkflowConfiguration updateWorkFlowConfiguration(Long workflowId,
			WorkflowConfigurationDTO workflowConfigurationDTO) {
		try {
			WorkflowConfiguration workflowConfiguration = workflowConfigurationRepository
					.findByWorkflowIdAndKey(workflowId, workflowConfigurationDTO.getKey())
					.orElseGet(() -> createWorkFlowConfiguration(workflowId, workflowConfigurationDTO));

			workflowConfiguration.setKey(workflowConfigurationDTO.getKey());
			workflowConfiguration.setValue(workflowConfigurationDTO.getValue());
			workflowConfiguration.setWorkflow(getWorkflowById(workflowId));
			return workflowConfigurationRepository.save(workflowConfiguration);
		} catch (WorkflowNotFoundException ex) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + workflowId);
		}
	}

	@Override
	public List<WorkflowConfigurationDTO> getWorkflowConfigurations(Long workflowId) {
		List<WorkflowConfiguration> configurations = workflowConfigurationRepository.findByWorkflowId(workflowId);
		List<WorkflowConfigurationDTO> dtos = new ArrayList<>();

		for (WorkflowConfiguration workflowConfiguration : configurations) {
			WorkflowConfigurationDTO workflowConfigurationDTO = new WorkflowConfigurationDTO();
			workflowConfigurationDTO.setId(workflowConfiguration.getId());
			workflowConfigurationDTO.setKey(workflowConfiguration.getKey());
			workflowConfigurationDTO.setValue(workflowConfiguration.getValue());
			workflowConfigurationDTO.setWorkflowId(workflowConfiguration.getWorkflow().getId());
			dtos.add(workflowConfigurationDTO);
		}

		return dtos;
	}

	@Override
	public Workflow getWorkflowByAlias(String alias) {
		Optional<Workflow> optionalWorkflow = workflowRepository.findByAlias(alias);
		return optionalWorkflow.orElse(null);
	}

	// private WorkflowStepDto convertToDto(WorkflowStep workflowStep) {
	// WorkflowStepDto dto = new WorkflowStepDto();
	// dto.setId(workflowStep.getId());
	// dto.setName(workflowStep.getName());
	// dto.setType(workflowStep.getType());
	// dto.setExecutionOrder(workflowStep.getExecutionOrder());
	// dto.setWorkflowStepConfigurations(
	// workflowStepConfigurationService.getWorkflowStepConfigurationDtoByWorkflowStepId(workflowStep.getId()));
	// dto.setWorkflowId(workflowStep.getWorkflow().getId());
	// return dto;
	// }

}

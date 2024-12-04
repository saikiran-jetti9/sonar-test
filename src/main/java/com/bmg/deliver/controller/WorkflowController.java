package com.bmg.deliver.controller;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.*;
import com.bmg.deliver.dto.responsedto.ResponseWorkflowInstanceDTO;
import com.bmg.deliver.dto.responsedto.WorkflowInstanceFilterDTO;
import com.bmg.deliver.exceptions.*;
import com.bmg.deliver.model.*;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.service.WorkflowService;
import com.bmg.deliver.serviceimpl.api.WorkflowServiceImpl;
import com.bmg.deliver.utils.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@Slf4j
@Conditional(MasterCondition.class)
public class WorkflowController {

	@Autowired
	WorkflowInstanceRepository workflowInstanceRepository;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private WorkflowInstanceService workflowInstanceService;

	@Autowired
	private WorkflowServiceImpl workflowMasterService;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	/**
	 * This method is used to retrieve all workflows
	 *
	 * @param page
	 * @param size
	 * @return Page<Workflow>
	 */
	@GetMapping
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns a list of all workflows", summary = "Retrieve all workflows")
	public Page<WorkflowDTO> listWorkflows(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false, defaultValue = "created") String sortBy,
			@RequestParam(required = false, defaultValue = "asc") String order,
			@RequestParam(required = false) String search, @RequestParam(required = false) Boolean enabled,
			@RequestParam(required = false) @DateTimeFormat(pattern = AppConstants.DATE_FORMAT_FULL_TIMESTAMP) Date startDate,
			@RequestParam(required = false) @DateTimeFormat(pattern = AppConstants.DATE_FORMAT_FULL_TIMESTAMP) Date endDate) {
		Sort sort = switch (sortBy) {
			case AppConstants.NAME -> AppConstants.ASC.equalsIgnoreCase(order)
					? Sort.by(Sort.Order.asc(AppConstants.NAME).ignoreCase())
					: Sort.by(Sort.Order.desc(AppConstants.NAME).ignoreCase());
			case AppConstants.ENABLED -> AppConstants.ASC.equalsIgnoreCase(order)
					? Sort.by(AppConstants.ENABLED).ascending()
					: Sort.by(AppConstants.ENABLED).descending();
			case AppConstants.CREATED -> AppConstants.ASC.equalsIgnoreCase(order)
					? Sort.by(AppConstants.CREATED).ascending()
					: Sort.by(AppConstants.CREATED).descending();
			default -> Sort.by("created").descending();
		};
		Pageable pageable = PageRequest.of(page, size, sort);
		return workflowService.getAllWorkflows(search, pageable, enabled, startDate, endDate);
	}

	/**
	 * This method is used to retrieve Workflow with specific Id
	 *
	 * @param id
	 * @return ResponseEntity<?> If successful, returns a ResponseEntity containing
	 *         the created Workflow;
	 * @throws WorkflowNotFoundException
	 *             if the specified workflow ID does not exist.
	 */
	@GetMapping("/{id}")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns details of a specific workflow by ID", summary = "Retrieve a specific workflow")
	public Workflow getWorkflow(@PathVariable Long id) {
		try {
			return workflowService.getWorkflowById(id);
		} catch (WorkflowNotFoundException e) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + id);
		}
	}

	/**
	 * This method is used to retrieve the workflow steps of the workflow with the
	 * specified ID
	 *
	 * @param id
	 * @return List<WorkflowStepDto>
	 * @throws WorkflowStepNotFoundException
	 */
	@GetMapping("/{id}/steps")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns a list of steps for a specific workflow", summary = "Retrieve workflow steps")
	public List<WorkflowStepDto> getWorkflowSteps(@PathVariable Long id) {
		return workflowService.getWorkflowSteps(id);

	}

	/**
	 * Creates a new workflow instance for the specified workflow ID based on the
	 * provided workflowInstanceDTO data.
	 *
	 * @param id
	 *            The ID of the workflow for which the instance is to be created.
	 * @param triggerData
	 *            The data transfer object containing the necessary information to
	 *            create a new workflow instance.
	 * @return ResponseEntity<?> If successful, returns a ResponseEntity containing
	 *         the created WorkflowInstance;
	 * @throws WorkflowNotFoundException
	 *             if the specified workflow ID does not exist.
	 * @throws WorkflowStepNotFoundException
	 *             if required workflow steps are missing or invalid.
	 */
	@PostMapping(value = {"alias/{alias}/instance", "/{id}/instance"})
	@Operation(tags = {
			"Workflow-Controller"}, description = "Creates a new instance for a specific workflow", summary = "Create a new workflow instance")
	@PreAuthorize("hasAnyRole('ROLE_APP_CLIENT') or hasAnyRole('ROLE_OKTA_USER')")
	public ResponseEntity<?> createWorkFlowInstance(@PathVariable(required = false) Long id,
			@PathVariable(required = false) String alias, @RequestBody JsonNode triggerData) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String triggerDataStr = objectMapper.writeValueAsString(triggerData);
			JsonObject triggerDataObj = JsonParser.parseString(triggerDataStr).getAsJsonObject();

			// If alias is provided, get the workflow ID from the alias
			Long workflowId = (id == null) ? workflowService.getWorkflowByAlias(alias).getId() : id;

			workflowMasterService.createWorkFlowInstance(workflowId, triggerDataObj);
			log.info("Before Web socket call ");
			// To update websockets
			TotalStatusCountDTO statusCounts = workflowInstanceService.retrieveTotalWorkflowsStatusCount();
			messagingTemplate.convertAndSend("/topic/workflow-status-counts", statusCounts);
			messagingTemplate.convertAndSend("/topic/workflow-updates",
					workflowInstanceService.retrieveStatusCountByWorkflow());
			log.info("After Web socket call ");

			return new ResponseEntity<>(HttpStatus.CREATED);
		} catch (WorkflowAliasNotFoundException e) {
			throw new WorkflowAliasNotFoundException(AppConstants.WORKFLOW_ALIAS_NOT_FOUND + alias);
		} catch (WorkflowNotFoundException ex) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + id);
		} catch (WorkflowStepNotFoundException e) {
			throw new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEPS_NOT_FOUND + id);
		} catch (WorkflowDisabledException e) {
			return ResponseEntity.ok(e.getMessage());
		} catch (Exception e) {
			log.error("Error creating workflow instance for workflowId: {}", id, e);
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/{id}/instance")
	@Operation(tags = {
			"Workflow-Instance-Controller"}, description = "Update a specific workflow instance", summary = "Updates a specific workflow instance by ID")
	public WorkflowInstance updateWorkflowInstance(@PathVariable Long id,
			@RequestBody WorkflowInstanceDTO workflowInstanceDTO) {
		try {
			WorkflowInstance workflowInstance = workflowService.updateWorkflowInstance(id, workflowInstanceDTO);
			TotalStatusCountDTO statusCounts = workflowInstanceService.retrieveTotalWorkflowsStatusCount();
			messagingTemplate.convertAndSend("/topic/workflow-status-counts", statusCounts);
			return workflowInstance;

		} catch (WorkflowInstancesNotFoundException e) {
			throw new WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id);
		}
	}

	@GetMapping("/alias/{alias}")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns details of a specific workflow by Alias", summary = "Retrieve a specific workflow by Alias")
	public Workflow getWorkflowByAlias(@PathVariable String alias) {
		try {
			return workflowService.getWorkflowByAlias(alias);
		} catch (WorkflowNotFoundException e) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ALIAS_NOT_FOUND + alias);
		}
	}

	/**
	 * This method is used to retrieve workflowInstances of the workflow with the
	 * specified ID
	 *
	 * @param id
	 *            The ID of the workflow for which instances are to be retrieved.
	 *            This ID should correspond to an * existing workflow in the system.
	 * @param page
	 *            page refers to page number of the response
	 * @param size
	 *            size refers to no of items in a page
	 * @param sortBy
	 *            sortBy refers to value by which the result should be order
	 * @param order
	 *            order refers to ascending or descending
	 * @param filter
	 *            filter is a dto which have filter parameters
	 * @return Page<WorkflowInstance>
	 * @throws WorkflowInstancesNotFoundException
	 */
	@GetMapping("/{id}/instances")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns a list of instances for a specific workflow", summary = "Retrieve workflow instances")
	public Page<ResponseWorkflowInstanceDTO> listWorkflowInstances(@PathVariable Long id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false, defaultValue = "id") String sortBy,
			@RequestParam(required = false, defaultValue = "asc") String order,
			@ModelAttribute WorkflowInstanceFilterDTO filter) {
		Sort sort = switch (sortBy) {
			case AppConstants.STATUS -> AppConstants.ASC.equalsIgnoreCase(order)
					? Sort.by(AppConstants.STATUS).ascending()
					: Sort.by(AppConstants.STATUS).descending();
			case AppConstants.PRIORITY -> AppConstants.ASC.equalsIgnoreCase(order)
					? Sort.by(AppConstants.PRIORITY).ascending()
					: Sort.by(AppConstants.PRIORITY).descending();
			case AppConstants.DURATION -> AppConstants.ASC.equalsIgnoreCase(order)
					? Sort.by(AppConstants.DURATION).ascending()
					: Sort.by(AppConstants.DURATION).descending();
			case AppConstants.CREATED -> AppConstants.ASC.equalsIgnoreCase(order)
					? Sort.by(AppConstants.CREATED).ascending()
					: Sort.by(AppConstants.CREATED).descending();
			default -> Sort.by(AppConstants.ID).descending();
		};
		Pageable pageable = PageRequest.of(page, size, sort);
		return workflowInstanceService.listWorkflowInstances(id, pageable, filter);
	}

	@GetMapping("/{id}/stats")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns a list of all instances for a specific workflow", summary = "Retrieve all workflow instances")
	public StatisticsDTO getWorkflowsStatistics(@PathVariable Long id) {
		return workflowInstanceService.getWorkflowsStatistics(id);
	}

	/**
	 * This method is used for creating workflow
	 *
	 * @param workflowDTO
	 * @return ResponseEntity<Workflow>
	 */
	@PostMapping
	@Operation(tags = {"Workflow-Controller"}, description = "Create a new workflow", summary = "Create a new workflow")
	public ResponseEntity<Workflow> createWorkflow(@RequestBody WorkflowDTO workflowDTO) {
		try {
			Workflow createdWorkflow = workflowService.createWorkflow(workflowDTO);
			return new ResponseEntity<>(createdWorkflow, HttpStatus.CREATED);
		} catch (WorkflowAliasExistsException e) {
			throw new WorkflowAliasExistsException(AppConstants.WORKFLOW_ALIAS_ALREADY_EXISTS + workflowDTO.getAlias());
		} catch (Exception e) {
			log.error("Error in creating workflow {}", ExceptionUtils.getStackTrace(e));
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR, e);
		}

	}

	/**
	 * This method is used to Update workflow with the specified ID
	 *
	 * @param id
	 * @param workflowDTO
	 * @return ResponseEntity<Workflow>
	 */
	@PutMapping("/{id}")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Update a specific workflow", summary = " Updates details of a specific workflow by ID")
	public ResponseEntity<Workflow> updateWorkflow(@PathVariable Long id, @RequestBody WorkflowDTO workflowDTO) {
		try {
			return ResponseEntity.ok(workflowService.updateWorkflow(id, workflowDTO));
		} catch (WorkflowNotFoundException e) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + id);
		}
	}

	/**
	 * This method is used to Delete workflow with the specified ID
	 *
	 * @param id
	 * @return String
	 */
	@DeleteMapping("/{id}")
	@Operation(tags = {
			"Workflow-Controller"}, description = " Delete a specific workflow", summary = " Delete a specific workflow by ID")
	public ResponseEntity<String> deleteWorkflow(@PathVariable Long id) {
		try {
			workflowService.deleteWorkflow(id);
			return ResponseEntity.ok(String.format(AppConstants.DELETED_WORKFLOW_SUCCESS, id));
		} catch (WorkflowNotFoundException e) {
			throw new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + id);
		} catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AppConstants.DELETED_WORKFLOW_ERROR);
		}
	}

	@PostMapping("/{workflowId}/configs")
	@Operation(tags = {
			"Workflow-Controller"}, description = " Create or update WorkflowConfiguration based on workflowId", summary = " Create WorkflowConfiguration based on workflowId")
	public ResponseEntity<WorkflowConfiguration> createWorkFlowConfiguration(@PathVariable Long workflowId,
			@RequestBody WorkflowConfigurationDTO workflowConfigurationDTO) {
		try {
			// Check if the workflow configuration already exists
			boolean exists = workflowService.getWorkflowConfigurations(workflowId).isEmpty();
			WorkflowConfiguration workflowConfiguration;

			if (!exists) {
				// Update existing configuration
				workflowConfiguration = workflowService.updateWorkFlowConfiguration(workflowId,
						workflowConfigurationDTO);
				return new ResponseEntity<>(workflowConfiguration, HttpStatus.OK);
			} else {
				// Create new configuration
				workflowConfiguration = workflowService.createWorkFlowConfiguration(workflowId,
						workflowConfigurationDTO);
				return new ResponseEntity<>(workflowConfiguration, HttpStatus.CREATED);
			}
		} catch (WorkflowNotFoundException ex) {
			throw new WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + workflowId);
		} catch (Exception e) {
			log.error("Error in creating workflowConfiguration {}", ExceptionUtils.getStackTrace(e));
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR, e);
		}
	}

	@GetMapping("/{workflowId}/configs")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns a list of workflow configurations for a specific workflow", summary = "Retrieve workflow configurations")
	public List<WorkflowConfigurationDTO> getWorkflowConfigurations(@PathVariable Long workflowId) {
		return workflowService.getWorkflowConfigurations(workflowId);
	}

	@GetMapping("/status/count")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns a count of workflow instances by status", summary = "Retrieve workflow status count")
	public TotalStatusCountDTO retrieveTotalWorkflowsStatusCount() {
		return workflowInstanceService.retrieveTotalWorkflowsStatusCount();
	}

	@GetMapping("/status/by-workflow")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Returns a count of workflow instances by status grouped by workflow", summary = "Retrieve status count by workflow")
	public List<WorkflowStatusCountDTO> retrieveStatusCountByWorkflow() {
		return workflowInstanceService.retrieveStatusCountByWorkflow();
	}

}

package com.bmg.deliver.controller;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.TotalStatusCountDTO;
import com.bmg.deliver.dto.responsedto.ResponseWorkflowInstanceDTO;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.exceptions.WorkflowInstanceIdNotFoundException;
import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.model.WorkflowInstanceArtifact;
import com.bmg.deliver.service.ArtifactService;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.utils.AppConstants;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@Conditional(MasterCondition.class)
@RequestMapping("/api/workflowinstance")
public class WorkflowInstanceController {

	@Autowired
	private WorkflowInstanceService workflowInstanceService;

	@Autowired
	private ArtifactService workflowInstanceArtifactService;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	/**
	 * This method is used to retrieve details of a workflow instance by its STATUS.
	 *
	 * @return WorkflowInstance
	 */
	@GetMapping
	@Operation(tags = {
			"Workflow-Instance-Controller"}, description = "Retrieve all workflow instances by status", summary = "Returns all workflowInstances based on status")
	public Page<ResponseWorkflowInstanceDTO> getWorkflowInstancesByStatus(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) WorkflowInstanceStatus status) {
		try {
			Pageable pageable = PageRequest.of(page, size);
			return workflowInstanceService.getWorkflowInstancesByStatus(pageable, status);
		} catch (Exception e) {
			throw new WorkflowInstancesNotFoundException(AppConstants.ERROR_RETRIEVING_INSTANCES);
		}
	}

	/**
	 * This method is used to retrieve details of a workflow instance by its ID.
	 *
	 * @param id
	 * @return WorkflowInstance
	 */
	@GetMapping("/{id}")
	@Operation(tags = {
			"Workflow-Instance-Controller"}, description = "Retrieve a specific workflow instance", summary = "Returns details of a specific workflow instance by ID")
	public Optional<WorkflowInstance> getWorkflowInstance(@PathVariable Long id) {
		try {
			return workflowInstanceService.getWorkflowInstanceById(id);
		} catch (WorkflowInstanceIdNotFoundException e) {
			throw new WorkflowInstanceIdNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id);
		}
	}

	@GetMapping("/{id}/artifacts")
	@Operation(tags = {
			"Workflow-Instance-Controller"}, description = "Returns artifacts for a specific workflow instance", summary = "Retrieve artifacts for a specific workflow instance")
	public Page<WorkflowInstanceArtifact> getWorkflowInstanceArtifacts(@PathVariable Long id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return workflowInstanceArtifactService.getWorkflowInstanceArtifacts(id, pageable);
	}

	@GetMapping("/{id}/logs")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Retrieve logs for a specific workflow instance", summary = " Returns logs for a specific workflow instance")
	public String getLogs(@PathVariable Long id) {
		try {
			return workflowInstanceService.getLogsOfWorkflowInstance(id);
		} catch (WorkflowInstancesNotFoundException e) {
			throw new WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id);
		}
	}

	@DeleteMapping("/{id}")
	@Operation(tags = {
			"Workflow-Instance-Controller"}, description = "Delete a specific workflow instance", summary = "Deletes a specific workflow instance by ID")
	public void deleteWorkflowInstance(@PathVariable Long id) {
		try {
			workflowInstanceService.deleteWorkflowInstance(id);
			TotalStatusCountDTO statusCounts = workflowInstanceService.retrieveTotalWorkflowsStatusCount();
			messagingTemplate.convertAndSend("/topic/workflow-status-counts", statusCounts);
		} catch (WorkflowInstancesNotFoundException e) {
			throw new WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id);
		}
	}
}

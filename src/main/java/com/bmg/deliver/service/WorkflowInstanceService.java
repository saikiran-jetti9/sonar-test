package com.bmg.deliver.service;

import com.bmg.deliver.dto.*;
import com.bmg.deliver.dto.responsedto.ResponseWorkflowInstanceDTO;
import com.bmg.deliver.dto.responsedto.WorkflowInstanceFilterDTO;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.nimbusds.jose.shaded.gson.JsonObject;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WorkflowInstanceService {
	WorkflowInstance createWorkFlowInstance(JsonObject triggerData, Workflow workflow);

	Page<ResponseWorkflowInstanceDTO> listWorkflowInstances(Long id, Pageable pageable,
			WorkflowInstanceFilterDTO filter);

	Optional<WorkflowInstance> getWorkflowInstanceById(Long instanceId);

	String getLogsOfWorkflowInstance(Long instanceId);

	Page<ResponseWorkflowInstanceDTO> getWorkflowsByIdentifier(String identifier, Pageable pageable);

	Page<ResponseWorkflowInstanceDTO> getWorkflowInstancesByStatus(Pageable pageable, WorkflowInstanceStatus status);

	void deleteWorkflowInstance(Long id);

	StatisticsDTO getWorkflowsStatistics(Long id);

	TotalStatusCountDTO retrieveTotalWorkflowsStatusCount();

	List<WorkflowStatusCountDTO> retrieveStatusCountByWorkflow();

	TotalStatusCountDTO getStatusCountByworkflow(Long id);

	void updateWorkflowInstanceStatus(Long id, WorkflowDTO workflowDTO);
}

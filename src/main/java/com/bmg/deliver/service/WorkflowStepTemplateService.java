package com.bmg.deliver.service;

import com.bmg.deliver.dto.WorkflowStepTemplateDTO;
import com.bmg.deliver.model.WorkflowStepTemplate;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepTemplateService {

	WorkflowStepTemplate createWorkflowStepTemplate(WorkflowStepTemplateDTO workflowStepTemplateDTO);

	WorkflowStepTemplate updateWorkflowStepTemplate(WorkflowStepTemplateDTO workflowStepTemplateDTO);

	List<WorkflowStepTemplateDTO> getWorkflowStepTemplates(Long workflowId);

	Optional<WorkflowStepTemplate> getWorkflowStepTemplate(Long workflowStepId);

}

package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.WorkflowStepTemplateDTO;
import com.bmg.deliver.exceptions.WorkflowStepNotFoundException;
import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.WorkflowStep;
import com.bmg.deliver.model.WorkflowStepTemplate;
import com.bmg.deliver.repository.WorkflowStepRepository;
import com.bmg.deliver.repository.WorkflowStepTemplateRepository;
import com.bmg.deliver.service.TemplateService;
import com.bmg.deliver.service.WorkflowStepTemplateService;
import com.bmg.deliver.utils.AppConstants;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowStepTemplateServiceImpl implements WorkflowStepTemplateService {

	@Autowired
	private WorkflowStepTemplateRepository workflowStepTemplateRepository;
	@Autowired
	private WorkflowStepRepository workflowStepRepository;

	@Autowired
	private Environment environment;
	@Autowired
	private TemplateService templateService;

	@Override
	public WorkflowStepTemplate createWorkflowStepTemplate(WorkflowStepTemplateDTO workflowStepTemplateDTO) {

		Template template = templateService.getTemplate(workflowStepTemplateDTO.getTemplateId());

		WorkflowStep workflowStep = getWorkflowStep(workflowStepTemplateDTO.getWorkflowStepId());

		WorkflowStepTemplate workflowStepTemplate = new WorkflowStepTemplate();
		workflowStepTemplate.setTemplateId(template.getId());
		workflowStepTemplate.setWorkflowStepId(workflowStep);

		return workflowStepTemplateRepository.save(workflowStepTemplate);
	}

	@Override
	public WorkflowStepTemplate updateWorkflowStepTemplate(WorkflowStepTemplateDTO workflowStepTemplateDTO) {

		WorkflowStep workflowStep = getWorkflowStep(workflowStepTemplateDTO.getWorkflowStepId());
		WorkflowStepTemplate workflowStepTemplate = workflowStepTemplateRepository
				.findByWorkflowStepId(workflowStepTemplateDTO.getWorkflowStepId())
				.orElseThrow(() -> new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_TEMPLATE_NOT_FOUND));
		workflowStepTemplate.setWorkflowStepId(workflowStep);
		workflowStepTemplate.setTemplateId(workflowStepTemplateDTO.getTemplateId());
		return workflowStepTemplateRepository.save(workflowStepTemplate);
	}

	@Override
	@Transactional
	public List<WorkflowStepTemplateDTO> getWorkflowStepTemplates(Long workflowId) {
		List<WorkflowStep> workflowSteps = workflowStepRepository.findByWorkflowId(workflowId);
		List<WorkflowStepTemplate> workflowStepTemplateList = workflowStepTemplateRepository
				.findByWorkflowStepIdIn(workflowSteps);
		return workflowStepTemplateList.stream().map(workflowStepTemplate -> {
			WorkflowStepTemplateDTO workflowStepTemplateDTO = new WorkflowStepTemplateDTO();
			workflowStepTemplateDTO
					.setName(templateService.getTemplate(workflowStepTemplate.getTemplateId()).getName());
			workflowStepTemplateDTO.setTemplateId(workflowStepTemplate.getTemplateId());
			workflowStepTemplateDTO.setWorkflowStepId(workflowStepTemplate.getWorkflowStepId().getId());
			return workflowStepTemplateDTO;
		}).toList();

	}

	@Override
	public Optional<WorkflowStepTemplate> getWorkflowStepTemplate(Long workflowStepId) {
		return workflowStepTemplateRepository.findByWorkflowStepId(workflowStepId);
	}

	public WorkflowStep getWorkflowStep(Long id) {

		Optional<WorkflowStep> optionalWorkflowStep = workflowStepRepository.findById(id);
		if (optionalWorkflowStep.isEmpty()) {
			throw new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_NOT_FOUND + id);
		}

		return optionalWorkflowStep.get();
	}
}

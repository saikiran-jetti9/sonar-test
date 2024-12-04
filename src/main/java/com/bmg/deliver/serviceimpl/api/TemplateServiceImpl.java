package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.TemplateDto;
import com.bmg.deliver.exceptions.TemplateIdNotFoundException;
import com.bmg.deliver.model.*;
import com.bmg.deliver.repository.TemplateRepository;
import com.bmg.deliver.repository.WorkflowStepTemplateRepository;
import com.bmg.deliver.service.TemplateService;
import com.bmg.deliver.service.TemplateVersionService;
import com.bmg.deliver.utils.AppConstants;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TemplateServiceImpl implements TemplateService {
	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	TemplateVersionService templateVersionService;

	@Autowired
	WorkflowStepTemplateRepository workflowStepTemplateRepository;

	@Override
	@Transactional
	public Template createTemplate(TemplateDto templateDto) {

		Template template = new Template();

		if (null != templateDto.getName()) {
			template.setName(templateDto.getName());
		}
		if (null != templateDto.getDescription()) {
			template.setDescription(templateDto.getDescription());
		}
		Template initialTemplate = templateRepository.save(template);
		templateDto.setTemplate(initialTemplate);
		TemplateVersion templateVersion = templateVersionService.createTemplateVersion(templateDto);
		template.setPrimaryVersionId(templateVersion.getId());

		return templateRepository.save(template);
	}

	@Override
	@Transactional
	public Template updateTemplate(Long id, TemplateDto templateDto) {
		Template template = templateRepository.findById(id)
				.orElseThrow(() -> new TemplateIdNotFoundException(AppConstants.TEMPLATE_ID_NOT_FOUND + id));
		if (null != templateDto.getName()) {
			template.setName(templateDto.getName());
		}
		if (null != templateDto.getDescription()) {
			template.setDescription(templateDto.getDescription());
		}
		if (null != templateDto.getTemplateCode()) {
			templateDto.setTemplate(template);
			TemplateVersion templateVersion = templateVersionService.createTemplateVersion(templateDto);
			template.setPrimaryVersionId(templateVersion.getId());
		}
		return templateRepository.save(template);
	}

	@Override
	public Template getTemplate(Long id) {
		return templateRepository.findById(id)
				.orElseThrow(() -> new TemplateIdNotFoundException(AppConstants.TEMPLATE_ID_NOT_FOUND + id));
	}

	@Override
	@Transactional
	public void deleteTemplate(Long id) {
		Template template = getTemplate(id);
		template.setPrimaryVersionId(null);
		workflowStepTemplateRepository.deleteByTemplateId(template.getId());
		templateVersionService.deleteByTemplateID(template.getId());
		templateRepository.deleteById(id);
	}

	@Override
	public Page<Template> getAllTemplates(Pageable pageable) {
		return templateRepository.findAll(pageable);
	}

	@Override
	public List<TemplateVersion> getTemplateVersions(Long id, Pageable pageable) {
		Optional<Template> template = templateRepository.findById(id);
		if (template.isPresent()) {
			return templateVersionService.getTemplateVersions(template.get(), pageable);
		} else {
			throw new TemplateIdNotFoundException(AppConstants.TEMPLATE_ID_NOT_FOUND + id);
		}
	}

	@Override
	public List<Workflow> getListWorkflows(Long id) {
		List<WorkflowStepTemplate> workflowStepTemplates = workflowStepTemplateRepository.findByTemplateId(id);
		List<Workflow> workflows = new ArrayList<>();
		for (WorkflowStepTemplate workflowStepTemplate : workflowStepTemplates) {
			workflows.add(workflowStepTemplate.getWorkflowStepId().getWorkflow());
		}
		return workflows;
	}
}

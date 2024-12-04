package com.bmg.deliver.workflow.step;

import com.bmg.deliver.config.WorkerCondition;
import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.model.*;
import com.bmg.deliver.repository.TemplateRepository;
import com.bmg.deliver.repository.TemplateVersionRepository;
import com.bmg.deliver.repository.WorkflowStepTemplateRepository;
import com.bmg.deliver.utils.ProductHelper;
import com.bmg.deliver.utils.StoreHelper;
import com.bmg.deliver.workflow.steps.*;
import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(WorkerCondition.class)
public class StepFactory {
	private final ProductHelper productHelper;

	private final StoreHelper storeHelper;
	private final WorkflowStepTemplateRepository workflowStepTemplateRepository;
	private final TemplateRepository templateRepository;
	private final TemplateVersionRepository templateVersionRepository;

	@Autowired
	StepFactory(ProductHelper productHelper, StoreHelper storeHelper,
			WorkflowStepTemplateRepository workflowStepTemplateRepository, TemplateRepository templateRepository,
			TemplateVersionRepository templateVersionRepository) {
		this.productHelper = productHelper;
		this.storeHelper = storeHelper;
		this.workflowStepTemplateRepository = workflowStepTemplateRepository;
		this.templateRepository = templateRepository;
		this.templateVersionRepository = templateVersionRepository;
	}

	public Step createStep(StepParams stepParams) {

		String templateCode = stepParams.getType().equals(WorkflowStepType.XML_RUNNER)
				|| stepParams.getType().equals(WorkflowStepType.DDEX) ? getTemplateCode(stepParams) : null;

		return switch (stepParams.getType()) {
			case SFTP -> new SFTPStep(stepParams);
			case DDEX -> new DDEXStep(stepParams, productHelper, storeHelper, templateCode);
			case GCS_UPLOADER -> new GCSUploaderStep(stepParams);
			case XML_RUNNER -> new XMLStep(stepParams, productHelper, templateCode);
		};
	}

	@Transactional
	private String getTemplateCode(StepParams stepParams) {
		Optional<WorkflowStepTemplate> workflowStepTemplate = workflowStepTemplateRepository
				.findByWorkflowStepId(stepParams.getId());
		if (workflowStepTemplate.isEmpty()) {
			return null;
		}
		Optional<Template> template = templateRepository.findById(workflowStepTemplate.get().getTemplateId());
		if (template.isEmpty()) {
			return null;
		}
		Optional<TemplateVersion> templateVersion = templateVersionRepository
				.findById(template.get().getPrimaryVersionId());
		return templateVersion.map(TemplateVersion::getTemplateCode).orElse(null);
	}
}

package com.bmg.deliver.service;

import com.bmg.deliver.dto.TemplateDto;
import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.TemplateVersion;
import com.bmg.deliver.model.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TemplateService {
	Template createTemplate(TemplateDto templateDto);

	Template updateTemplate(Long id, TemplateDto templateDto);

	Template getTemplate(Long id);

	void deleteTemplate(Long id);

	Page<Template> getAllTemplates(Pageable pageable);

	List<TemplateVersion> getTemplateVersions(Long id, Pageable pageable);

	List<Workflow> getListWorkflows(Long id);
}

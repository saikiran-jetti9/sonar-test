package com.bmg.deliver.service;

import com.bmg.deliver.dto.TemplateDto;
import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.TemplateVersion;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TemplateVersionService {

	List<TemplateVersion> getTemplateVersions(Template template, Pageable pageable);

	TemplateVersion createTemplateVersion(TemplateDto templateDto);

	void deleteByTemplateID(Long id);
}

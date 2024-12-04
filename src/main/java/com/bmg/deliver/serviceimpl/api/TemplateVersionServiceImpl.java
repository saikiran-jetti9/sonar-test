package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.TemplateDto;
import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.TemplateVersion;
import com.bmg.deliver.repository.TemplateVersionRepository;
import com.bmg.deliver.service.TemplateVersionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TemplateVersionServiceImpl implements TemplateVersionService {

	private static final int MAX_VERSIONS = 20;

	@Autowired
	private TemplateVersionRepository templateVersionRepository;

	@Override
	public List<TemplateVersion> getTemplateVersions(Template template, Pageable pageable) {
		return templateVersionRepository.findLatestByTemplate(template.getId());
	}

	@Override
	@Transactional
	public TemplateVersion createTemplateVersion(TemplateDto templateDto) {

		TemplateVersion templateVersion = new TemplateVersion();
		templateVersion.setTemplate(templateDto.getTemplate());
		templateVersion.setTemplateCode(templateDto.getTemplateCode());
		templateVersion.setTemplateDescription(templateDto.getTemplateDescription());
		templateVersion.setCreated(new Date());
		templateVersion.setModified(new Date());

		List<TemplateVersion> existingVersions = templateVersionRepository
				.findLatestByTemplate(templateDto.getTemplate().getId());

		List<TemplateVersion> sortedVersions = existingVersions.stream()
				.sorted((v1, v2) -> v2.getCreated().compareTo(v1.getCreated())).toList();

		if (sortedVersions.size() > MAX_VERSIONS) {
			List<TemplateVersion> versionsToDelete = sortedVersions.subList(MAX_VERSIONS, sortedVersions.size());
			templateVersionRepository.deleteAll(versionsToDelete);
		}
		return templateVersionRepository.save(templateVersion);
	}

	@Override
	public void deleteByTemplateID(Long id) {
		templateVersionRepository.deleteByTemplateId(id);

	}
}

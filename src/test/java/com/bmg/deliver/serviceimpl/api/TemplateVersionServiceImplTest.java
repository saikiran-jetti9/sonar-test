package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.TemplateDto;
import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.TemplateVersion;
import com.bmg.deliver.repository.TemplateVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemplateVersionServiceImplTest {

	@Mock
	private TemplateVersionRepository templateVersionRepository;

	@InjectMocks
	private TemplateVersionServiceImpl templateVersionService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void getTemplateVersions() {
		Template template = new Template();
		Pageable pageable = PageRequest.of(0, 10);
		List<TemplateVersion> versions = List.of(new TemplateVersion(), new TemplateVersion());
		Page<TemplateVersion> page = new PageImpl<>(versions, pageable, versions.size());
		when(templateVersionRepository.findByTemplate(template, pageable)).thenReturn(page);

		List<TemplateVersion> result = templateVersionService.getTemplateVersions(template, pageable);

		assertNotNull(result);
	}

	@Test
	void createTemplateVersion_Success() {
		Template template = new Template();
		TemplateDto templateDto = new TemplateDto();
		templateDto.setTemplate(template);
		templateDto.setTemplateCode("code123");
		templateDto.setDescription("Description");

		TemplateVersion newTemplateVersion = new TemplateVersion();
		newTemplateVersion.setTemplate(template);
		newTemplateVersion.setTemplateCode("code123");
		newTemplateVersion.setTemplateDescription("Description");
		newTemplateVersion.setCreated(new Date());
		newTemplateVersion.setModified(new Date());

		List<TemplateVersion> existingVersions = List.of(newTemplateVersion);
		when(templateVersionRepository.findLatestByTemplate(template.getId())).thenReturn(existingVersions);
		when(templateVersionRepository.save(any(TemplateVersion.class))).thenReturn(newTemplateVersion);

		TemplateVersion result = templateVersionService.createTemplateVersion(templateDto);

		assertNotNull(result);
		assertEquals(newTemplateVersion.getTemplateCode(), result.getTemplateCode());
		verify(templateVersionRepository, times(1)).save(any(TemplateVersion.class));
	}

	@Test
	void createTemplateVersion_DeleteOldVersions() {
		Template template = new Template();
		TemplateDto templateDto = new TemplateDto();
		templateDto.setTemplate(template);
		templateDto.setTemplateCode("code123");
		templateDto.setDescription("Description");

		TemplateVersion oldTemplateVersion1 = new TemplateVersion();
		oldTemplateVersion1.setCreated(new Date());

		List<TemplateVersion> existingVersions = List.of(oldTemplateVersion1);
		when(templateVersionRepository.findLatestByTemplate(template.getId())).thenReturn(existingVersions);

		TemplateVersion newTemplateVersion = new TemplateVersion();
		newTemplateVersion.setTemplate(template);
		newTemplateVersion.setTemplateCode("code123");
		newTemplateVersion.setTemplateDescription("Description");
		newTemplateVersion.setCreated(new Date());
		newTemplateVersion.setModified(new Date());

		when(templateVersionRepository.save(any(TemplateVersion.class))).thenReturn(newTemplateVersion);

		TemplateVersion result = templateVersionService.createTemplateVersion(templateDto);

		assertNotNull(result);
		verify(templateVersionRepository, times(1)).save(any(TemplateVersion.class));
	}

	@Test
	void deleteByTemplateID() {
		Long templateId = 1L;
		templateVersionService.deleteByTemplateID(templateId);
		verify(templateVersionRepository, times(1)).deleteByTemplateId(templateId);
	}
}

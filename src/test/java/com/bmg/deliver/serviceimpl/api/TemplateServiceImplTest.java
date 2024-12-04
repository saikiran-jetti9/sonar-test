package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.TemplateDto;
import com.bmg.deliver.exceptions.TemplateIdNotFoundException;
import com.bmg.deliver.model.*;
import com.bmg.deliver.repository.TemplateRepository;
import com.bmg.deliver.repository.WorkflowStepTemplateRepository;
import com.bmg.deliver.service.TemplateVersionService;
import com.bmg.deliver.utils.AppConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class TemplateServiceImplTest {

	@Mock
	private TemplateRepository templateRepository;

	@Mock
	private WorkflowStepTemplateRepository workflowStepTemplateRepository;

	@Mock
	private TemplateVersionService templateVersionService;

	@InjectMocks
	private TemplateServiceImpl templateServiceImpl;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	@Transactional
	void testCreateTemplate() {
		TemplateDto templateDto = new TemplateDto();
		templateDto.setName("Test Template");
		templateDto.setDescription("Test Description");

		Template initialTemplate = new Template();
		initialTemplate.setName("Test Template");
		initialTemplate.setDescription("Test Description");

		Template savedTemplate = new Template();
		savedTemplate.setId(1L);
		savedTemplate.setName("Test Template");
		savedTemplate.setDescription("Test Description");

		TemplateVersion templateVersion = new TemplateVersion();
		templateVersion.setId(2L);

		doReturn(savedTemplate).when(templateRepository).save(any(Template.class));
		doReturn(templateVersion).when(templateVersionService).createTemplateVersion(any(TemplateDto.class));

		Template result = templateServiceImpl.createTemplate(templateDto);

		assertEquals(savedTemplate, result);
		verify(templateVersionService).createTemplateVersion(eq(templateDto));
	}

	@Test
	void testUpdateTemplate() {
		Long templateId = 1L;
		TemplateDto templateDto = new TemplateDto();
		templateDto.setName("Updated Template");
		templateDto.setDescription("Updated Description");
		templateDto.setTemplateCode("<xml>..</xml>");

		Template existingTemplate = new Template();
		existingTemplate.setId(templateId);
		existingTemplate.setName("Old Template");
		existingTemplate.setDescription("Old Description");

		Template updatedTemplate = new Template();
		updatedTemplate.setId(templateId);
		updatedTemplate.setName("Updated Template");
		updatedTemplate.setDescription("Updated Description");

		TemplateVersion templateVersion = new TemplateVersion();
		templateVersion.setId(2L);

		when(templateRepository.findById(templateId)).thenReturn(Optional.of(existingTemplate));
		doReturn(templateVersion).when(templateVersionService).createTemplateVersion(any(TemplateDto.class));
		when(templateRepository.save(any(Template.class))).thenReturn(updatedTemplate);

		Template result = templateServiceImpl.updateTemplate(templateId, templateDto);

		assertEquals(updatedTemplate, result);
	}

	@Test
	void testUpdateTemplate_TemplateIdNotFound() {
		Long id = 1L;
		Template template = new Template();
		TemplateDto templateDto = new TemplateDto();
		when(templateRepository.findById(anyLong())).thenReturn(Optional.empty());

		TemplateIdNotFoundException thrown = assertThrows(TemplateIdNotFoundException.class, () -> {
			templateServiceImpl.updateTemplate(id, templateDto);
		});

		assertEquals(AppConstants.TEMPLATE_ID_NOT_FOUND + id, thrown.getMessage());
	}

	@Test
	void testGetTemplate_Success() {
		Long id = 1L;
		Template template = new Template();
		when(templateRepository.findById(anyLong())).thenReturn(Optional.of(template));

		Template result = templateServiceImpl.getTemplate(id);

		assertNotNull(result);
		verify(templateRepository).findById(id);
	}

	@Test
	void testGetTemplate_TemplateIdNotFound() {
		Long id = 1L;
		when(templateRepository.findById(anyLong())).thenReturn(Optional.empty());

		TemplateIdNotFoundException thrown = assertThrows(TemplateIdNotFoundException.class, () -> {
			templateServiceImpl.getTemplate(id);
		});

		assertEquals(AppConstants.TEMPLATE_ID_NOT_FOUND + id, thrown.getMessage());
	}

	@Test
	void testDeleteTemplate() {
		Long templateId = 1L;
		Template template = new Template();
		template.setId(templateId);
		template.setPrimaryVersionId(2L);

		when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
		doNothing().when(workflowStepTemplateRepository).deleteByTemplateId(templateId);
		doNothing().when(templateVersionService).deleteByTemplateID(templateId);
		doNothing().when(templateRepository).deleteById(templateId);

		templateServiceImpl.deleteTemplate(templateId);

		verify(templateRepository).findById(templateId);
		verify(workflowStepTemplateRepository).deleteByTemplateId(templateId);
		verify(templateVersionService).deleteByTemplateID(templateId);
		verify(templateRepository).deleteById(templateId);
	}

	@Test
	void testDeleteTemplate_TemplateIdNotFound() {
		Long id = 1L;
		when(templateRepository.findById(anyLong())).thenReturn(Optional.empty());

		TemplateIdNotFoundException thrown = assertThrows(TemplateIdNotFoundException.class, () -> {
			templateServiceImpl.deleteTemplate(id);
		});

		assertEquals(AppConstants.TEMPLATE_ID_NOT_FOUND + id, thrown.getMessage());
	}

	@Test
	void testGetAllTemplates() {
		Pageable pageable = PageRequest.of(0, 10);
		Page<Template> page = new PageImpl<>(Collections.singletonList(new Template()), pageable, 1);
		when(templateRepository.findAll(any(Pageable.class))).thenReturn(page);

		Page<Template> result = templateServiceImpl.getAllTemplates(pageable);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		verify(templateRepository).findAll(pageable);
	}

	@Test
	void testGetTemplateVersions_Success() {
		Long id = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		Template template = new Template();
		List<TemplateVersion> page = Collections.singletonList(new TemplateVersion());
		when(templateRepository.findById(anyLong())).thenReturn(Optional.of(template));
		when(templateVersionService.getTemplateVersions(any(Template.class), any(Pageable.class))).thenReturn(page);

		List<TemplateVersion> result = templateServiceImpl.getTemplateVersions(id, pageable);

		assertNotNull(result);
		verify(templateRepository).findById(id);
		verify(templateVersionService).getTemplateVersions(template, pageable);
	}

	@Test
	void testGetTemplateVersions_TemplateIdNotFound() {
		Long id = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		when(templateRepository.findById(anyLong())).thenReturn(Optional.empty());

		TemplateIdNotFoundException thrown = assertThrows(TemplateIdNotFoundException.class, () -> {
			templateServiceImpl.getTemplateVersions(id, pageable);
		});

		assertEquals(AppConstants.TEMPLATE_ID_NOT_FOUND + id, thrown.getMessage());
	}

	@Test
	void testGetListWorkflows() {
		Long templateId = 1L;
		Workflow workflow1 = new Workflow();
		Workflow workflow2 = new Workflow();

		WorkflowStep workflowStep1 = new WorkflowStep();
		workflowStep1.setWorkflow(workflow1);

		WorkflowStep workflowStep2 = new WorkflowStep();
		workflowStep2.setWorkflow(workflow2);

		WorkflowStepTemplate stepTemplate1 = new WorkflowStepTemplate();
		stepTemplate1.setWorkflowStepId(workflowStep1);

		WorkflowStepTemplate stepTemplate2 = new WorkflowStepTemplate();
		stepTemplate2.setWorkflowStepId(workflowStep2);

		List<WorkflowStepTemplate> stepTemplates = new ArrayList<>();
		stepTemplates.add(stepTemplate1);
		stepTemplates.add(stepTemplate2);

		when(workflowStepTemplateRepository.findByTemplateId(templateId)).thenReturn(stepTemplates);

		List<Workflow> result = templateServiceImpl.getListWorkflows(templateId);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(workflow1, result.get(0));
		assertEquals(workflow2, result.get(1));
		verify(workflowStepTemplateRepository).findByTemplateId(templateId);
	}

}

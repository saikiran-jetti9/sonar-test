package com.bmg.deliver.controller;

import com.bmg.deliver.dto.TemplateDto;
import com.bmg.deliver.dto.WorkflowStepTemplateDTO;
import com.bmg.deliver.exceptions.ApplicationRuntimeException;
import com.bmg.deliver.exceptions.TemplateIdNotFoundException;
import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.TemplateVersion;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.service.TemplateService;
import com.bmg.deliver.service.WorkflowStepTemplateService;
import com.bmg.deliver.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemplateControllerTest {

	@Mock
	private TemplateService templateService;

	@Mock
	private WorkflowStepTemplateService workflowStepTemplateService;

	@InjectMocks
	private TemplateController templateController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void createTemplate_Success() {
		TemplateDto templateDto = new TemplateDto();
		Template template = new Template();
		when(templateService.createTemplate(templateDto)).thenReturn(template);

		Template result = templateController.createTemplate(templateDto);
		assertNotNull(result);
		assertEquals(template, result);
	}

	@Test
	void createTemplate_Exception() {
		TemplateDto templateDto = new TemplateDto();
		when(templateService.createTemplate(templateDto)).thenThrow(new RuntimeException("Database error"));

		Exception exception = assertThrows(ApplicationRuntimeException.class, () -> {
			templateController.createTemplate(templateDto);
		});

		assertEquals(AppConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
	}

	@Test
	void updateTemplate_Success() {
		Long id = 1L;
		TemplateDto templateDto = new TemplateDto();
		Template template = new Template();
		when(templateService.updateTemplate(id, templateDto)).thenReturn(template);

		Template result = templateController.updateTemplate(id, templateDto);
		assertNotNull(result);
		assertEquals(template, result);
	}

	@Test
	void updateTemplate_TemplateNotFound() {
		Long id = 1L;
		TemplateDto templateDto = new TemplateDto();
		when(templateService.updateTemplate(id, templateDto))
				.thenThrow(new TemplateIdNotFoundException(AppConstants.TEMPLATE_ID_NOT_FOUND + id));

		Exception exception = assertThrows(TemplateIdNotFoundException.class, () -> {
			templateController.updateTemplate(id, templateDto);
		});

		assertEquals(AppConstants.TEMPLATE_ID_NOT_FOUND + id, exception.getMessage());
	}

	@Test
	void testUpdateTemplateInternalError() {
		Long id = 1L;
		TemplateDto templateDto = new TemplateDto();

		when(templateService.updateTemplate(id, templateDto)).thenThrow(new RuntimeException("Unexpected error"));

		ApplicationRuntimeException exception = assertThrows(ApplicationRuntimeException.class, () -> {
			templateController.updateTemplate(id, templateDto);
		});

		assertEquals(AppConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
	}

	@Test
	void getTemplate_Success() {
		Long id = 1L;
		Template template = new Template();
		when(templateService.getTemplate(id)).thenReturn(template);

		Template result = templateController.getTemplate(id);
		assertNotNull(result);
		assertEquals(template, result);
	}

	@Test
	void getTemplate_TemplateNotFound() {
		Long id = 1L;
		when(templateService.getTemplate(id))
				.thenThrow(new TemplateIdNotFoundException(AppConstants.TEMPLATE_ID_NOT_FOUND + id));

		Exception exception = assertThrows(TemplateIdNotFoundException.class, () -> {
			templateController.getTemplate(id);
		});

		assertEquals(AppConstants.TEMPLATE_ID_NOT_FOUND + id, exception.getMessage());
	}

	@Test
	void testGetTemplateInternalError() {
		Long id = 1L;

		when(templateService.getTemplate(id)).thenThrow(new RuntimeException("Unexpected error"));

		ApplicationRuntimeException exception = assertThrows(ApplicationRuntimeException.class, () -> {
			templateController.getTemplate(id);
		});

		assertEquals(AppConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
	}
	@Test
	void deleteTemplate_Success() {
		Long id = 1L;
		doNothing().when(templateService).deleteTemplate(id);

		ResponseEntity<String> response = templateController.deleteTemplate(id);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Template Deleted Successfully with id: " + id, response.getBody());
	}

	@Test
	void deleteTemplate_TemplateNotFound() {
		Long id = 1L;
		doThrow(new TemplateIdNotFoundException(AppConstants.TEMPLATE_ID_NOT_FOUND + id)).when(templateService)
				.deleteTemplate(id);

		ResponseEntity<String> response = templateController.deleteTemplate(id);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals(AppConstants.TEMPLATE_ID_NOT_FOUND + id, response.getBody());
	}

	@Test
	void deleteTemplate_DataIntegrityViolation() {
		Long id = 1L;
		doThrow(new DataIntegrityViolationException("Error")).when(templateService).deleteTemplate(id);

		ResponseEntity<String> response = templateController.deleteTemplate(id);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(AppConstants.DELETED_TEMPLATE_ERROR, response.getBody());
	}

	@Test
	void deleteTemplate_InternalError() {
		Long id = 1L;
		doThrow(new RuntimeException("Unexpected error")).when(templateService).deleteTemplate(id);
		ResponseEntity<String> response = templateController.deleteTemplate(id);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("An unexpected error occurred.", response.getBody());
	}

	@Test
	void getTemplates_Success() {
		Pageable pageable = PageRequest.of(0, 10);
		Page<Template> page = new PageImpl<>(Arrays.asList(new Template(), new Template()), pageable, 2);
		when(templateService.getAllTemplates(pageable)).thenReturn(page);

		Page<Template> result = templateController.getTemplates(0, 10);
		assertNotNull(result);
		assertEquals(2, result.getTotalElements());
		assertEquals(2, result.getContent().size());
	}

	@Test
	void getTemplates_Error() {
		Pageable pageable = PageRequest.of(0, 10);
		when(templateService.getAllTemplates(pageable))
				.thenThrow(new WorkflowInstancesNotFoundException(AppConstants.ERROR_RETRIEVING_INSTANCES));

		Exception exception = assertThrows(WorkflowInstancesNotFoundException.class, () -> {
			templateController.getTemplates(0, 10);
		});

		assertEquals(AppConstants.ERROR_RETRIEVING_INSTANCES, exception.getMessage());
	}

	@Test
	void getTemplateVersions_Success() {
		Long id = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		List<TemplateVersion> list = Arrays.asList(new TemplateVersion());
		when(templateService.getTemplateVersions(id, pageable)).thenReturn(list);

		List<TemplateVersion> result = templateController.getTemplateVersions(0, 10, id);
		assertNotNull(result);
	}

	@Test
	void getTemplateVersions_Error() {
		Long id = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		when(templateService.getTemplateVersions(id, pageable))
				.thenThrow(new WorkflowInstancesNotFoundException(AppConstants.ERROR_RETRIEVING_INSTANCES));

		Exception exception = assertThrows(WorkflowInstancesNotFoundException.class, () -> {
			templateController.getTemplateVersions(0, 10, id);
		});

		assertEquals(AppConstants.ERROR_RETRIEVING_INSTANCES, exception.getMessage());
	}

	@Test
	void getListOfWorkflow_Success() {
		Long id = 1L;
		List<Workflow> workflows = Arrays.asList(new Workflow(), new Workflow());
		when(templateService.getListWorkflows(id)).thenReturn(workflows);

		List<Workflow> result = templateController.getListOfWorkflow(id);
		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void getListOfWorkflow_Error() {
		Long id = 1L;
		when(templateService.getListWorkflows(id))
				.thenThrow(new WorkflowInstancesNotFoundException(AppConstants.ERROR_RETRIEVING_INSTANCES));

		Exception exception = assertThrows(WorkflowInstancesNotFoundException.class, () -> {
			templateController.getListOfWorkflow(id);
		});

		assertEquals(AppConstants.ERROR_RETRIEVING_INSTANCES, exception.getMessage());
	}

	@Test
	void getWorkflowStepTemplates() {
		List<WorkflowStepTemplateDTO> workflowStepTemplates = new ArrayList<>();
		WorkflowStepTemplateDTO workflowStepTemplate = new WorkflowStepTemplateDTO();
		workflowStepTemplate.setWorkflowStepId(1L);
		workflowStepTemplates.add(workflowStepTemplate);

		when(workflowStepTemplateService.getWorkflowStepTemplates(anyLong())).thenReturn(workflowStepTemplates);

		assertEquals(workflowStepTemplates, templateController.getWorkflowStepTemplates(1L));
	}

}

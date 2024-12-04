package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.WorkflowStepTemplateDTO;
import com.bmg.deliver.exceptions.WorkflowStepNotFoundException;
import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStep;
import com.bmg.deliver.model.WorkflowStepTemplate;
import com.bmg.deliver.repository.WorkflowStepRepository;
import com.bmg.deliver.repository.WorkflowStepTemplateRepository;
import com.bmg.deliver.service.TemplateService;
import com.bmg.deliver.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowStepTemplateServiceImplTest {

	private static final Logger log = LoggerFactory.getLogger(WorkflowStepTemplateServiceImplTest.class);
	@Mock
	private WorkflowStepTemplateRepository workflowStepTemplateRepository;

	@Mock
	private WorkflowStepRepository workflowStepRepository;

	@Mock
	private TemplateService templateService;

	@Mock
	private Environment environment;

	@InjectMocks
	private WorkflowStepTemplateServiceImpl workflowStepTemplateService;

	private WorkflowStepTemplateDTO workflowStepTemplateDTO;
	private Template template;
	private WorkflowStep workflowStep;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		workflowStepTemplateDTO = new WorkflowStepTemplateDTO();
		workflowStepTemplateDTO.setTemplateId(1L);
		workflowStepTemplateDTO.setWorkflowStepId(1L);

		template = new Template();
		template.setId(1L);
		template.setName("Test Template");

		workflowStep = new WorkflowStep();
		workflowStep.setId(1L);
	}

	@Test
	void getWorkflowStep_Success() {
		Long workflowStepId = 2L;
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setId(workflowStepId);

		when(workflowStepRepository.findById(workflowStepId)).thenReturn(Optional.of(workflowStep));

		WorkflowStep result = workflowStepTemplateService.getWorkflowStep(workflowStepId);

		assertNotNull(result);
		assertEquals(workflowStepId, result.getId());
	}

	@Test
	void getWorkflowStep_NotFound() {
		Long workflowStepId = 2L;

		when(workflowStepRepository.findById(workflowStepId)).thenReturn(Optional.empty());

		WorkflowStepNotFoundException thrown = assertThrows(WorkflowStepNotFoundException.class,
				() -> workflowStepTemplateService.getWorkflowStep(workflowStepId),
				"Expected getWorkflowStep() to throw, but it didn't");
		assertEquals(AppConstants.WORKFLOW_STEP_NOT_FOUND + workflowStepId, thrown.getMessage());
	}

	@Test
	void testGetWorkflowStepTemplates() {
		Workflow workflow = new Workflow();
		workflow.setId(1L);

		List<WorkflowStep> workflowSteps = new ArrayList<>();
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setWorkflow(workflow);
		workflowSteps.add(workflowStep);

		List<WorkflowStepTemplate> workflowStepTemplates = new ArrayList<>();
		WorkflowStepTemplate workflowStepTemplate = new WorkflowStepTemplate();
		workflowStepTemplate.setTemplateId(1L);
		workflowStepTemplate.setWorkflowStepId(workflowStep);
		workflowStepTemplates.add(workflowStepTemplate);

		when(workflowStepRepository.findByWorkflowId(1L)).thenReturn(workflowSteps);
		when(workflowStepTemplateRepository.findByWorkflowStepIdIn(workflowSteps)).thenReturn(workflowStepTemplates);
		when(templateService.getTemplate(1L)).thenReturn(new Template());

		List<WorkflowStepTemplateDTO> result = workflowStepTemplateService.getWorkflowStepTemplates(1L);

		assertNotNull(result);
	}

	@Test
	void createWorkflowStepTemplate_ShouldReturnSavedTemplate() {
		when(templateService.getTemplate(workflowStepTemplateDTO.getTemplateId())).thenReturn(template);
		when(workflowStepRepository.findById(workflowStepTemplateDTO.getWorkflowStepId())).thenReturn(Optional.of(workflowStep));
		when(workflowStepTemplateRepository.save(any(WorkflowStepTemplate.class))).thenReturn(new WorkflowStepTemplate());

		WorkflowStepTemplate result = workflowStepTemplateService.createWorkflowStepTemplate(workflowStepTemplateDTO);

		assertNotNull(result);
		verify(templateService).getTemplate(workflowStepTemplateDTO.getTemplateId());
		verify(workflowStepRepository).findById(workflowStepTemplateDTO.getWorkflowStepId());
		verify(workflowStepTemplateRepository).save(any(WorkflowStepTemplate.class));
	}

	@Test
	void updateWorkflowStepTemplate_ShouldUpdateAndReturnTemplate() {
		WorkflowStepTemplate existingTemplate = new WorkflowStepTemplate();
		existingTemplate.setTemplateId(1L);
		existingTemplate.setWorkflowStepId(workflowStep);

		when(workflowStepRepository.findById(workflowStepTemplateDTO.getWorkflowStepId()))
				.thenReturn(Optional.of(workflowStep));
		when(workflowStepTemplateRepository.findByWorkflowStepId(workflowStepTemplateDTO.getWorkflowStepId()))
				.thenReturn(Optional.of(existingTemplate));
		when(workflowStepTemplateRepository.save(any(WorkflowStepTemplate.class))).thenReturn(existingTemplate);

		WorkflowStepTemplate result = workflowStepTemplateService.updateWorkflowStepTemplate(workflowStepTemplateDTO);

		assertNotNull(result);
		assertEquals(existingTemplate.getTemplateId(), result.getTemplateId());
		verify(workflowStepTemplateRepository).findByWorkflowStepId(workflowStepTemplateDTO.getWorkflowStepId());
	}

	@Test
	void updateWorkflowStepTemplate_ShouldThrowException_WhenWorkflowStepNotFound() {
		when(workflowStepRepository.findById(workflowStepTemplateDTO.getWorkflowStepId())).thenReturn(Optional.empty());

		assertThrows(WorkflowStepNotFoundException.class, () -> {
			workflowStepTemplateService.updateWorkflowStepTemplate(workflowStepTemplateDTO);
		});
	}
}

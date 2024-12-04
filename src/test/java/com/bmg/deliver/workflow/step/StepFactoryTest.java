package com.bmg.deliver.workflow.step;

import static org.junit.Assert.assertTrue;

import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.repository.TemplateRepository;
import com.bmg.deliver.repository.TemplateVersionRepository;
import com.bmg.deliver.repository.WorkflowStepTemplateRepository;
import com.bmg.deliver.utils.ProductHelper;
import com.bmg.deliver.utils.StoreHelper;
import com.bmg.deliver.workflow.steps.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StepFactoryTest {

	private StepFactory stepFactory;

	@Mock
	private ProductHelper productHelper;

	@Mock
	private StoreHelper storeHelper;

	@Mock
	private WorkflowStepTemplateRepository workflowStepTemplateRepository;
	@Mock
	private TemplateRepository templateRepository;
	@Mock
	private TemplateVersionRepository templateVersionRepository;

	private StepParams stepParams;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		stepParams = new StepParams(2L, new Workflow(), 2, "DDEX Step", WorkflowStepType.DDEX,
				List.of(new WorkflowStepConfiguration()));
		stepFactory = new StepFactory(productHelper, storeHelper, workflowStepTemplateRepository, templateRepository,
				templateVersionRepository);
	}

	@Test
	void testCreateDDEXStep() {
		Step step = stepFactory.createStep(stepParams);
		assertTrue(step instanceof DDEXStep);
	}

	@Test
	void testCreateXMLStep() {
		StepParams stepParams = new StepParams(5L, new Workflow(), 5, "XML Step", WorkflowStepType.XML_RUNNER,
				List.of(new WorkflowStepConfiguration()));
		Step step = stepFactory.createStep(stepParams);
		assertTrue(step instanceof XMLStep);
	}
}

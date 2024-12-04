package com.bmg.deliver.workflow.steps;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import com.bmg.deliver.enums.ReleaseType;
import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.utils.ProductHelper;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.bmg.deliver.workflow.execution.Logger;
import com.bmg.deliver.workflow.step.StepParams;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class XMLStepTest {

	@Mock
	private Workflow workflow;

	@Mock
	private Logger logger;

	@Mock
	private ProductHelper productHelper;

	@Mock
	private ExecutionContext context;
	private XMLStep xmlStep;

	private Gson gson = new Gson();
	@Mock
	private ReleaseType releaseType;

	@BeforeEach
	void setUp() {
		List<WorkflowStepConfiguration> stepConfigurations = new ArrayList<>();
		StepParams params = new StepParams(1L, workflow, 1, "Test Step", WorkflowStepType.DDEX, stepConfigurations);
		xmlStep = new XMLStep(params, productHelper, "testTemplateCode");
		ReflectionTestUtils.setField(xmlStep, "logger", logger);
		ReflectionTestUtils.setField(xmlStep, "context", context);
		ReflectionTestUtils.setField(xmlStep, "releaseType", ReleaseType.BATCHED);
	}

	@Test
	void testInitConfigurations() {
		List<WorkflowStepConfiguration> stepConfigurations = List.of();
		xmlStep.initConfigurations(stepConfigurations);
		assertNotNull(stepConfigurations);
	}

	@Test
	void testInitConfigurationsWithStepConfigurations() {
		WorkflowStepConfiguration config1 = new WorkflowStepConfiguration();
		config1.setId(1L);
		config1.setKey("DDEX_RELEASE_TYPE");
		config1.setValue("SINGLE");
		config1.setCreated(new Date());
		config1.setModified(new Date());
		List<WorkflowStepConfiguration> stepConfigurations = new ArrayList<>();
		stepConfigurations.add(config1);
		xmlStep.initConfigurations(stepConfigurations);
		assertNotNull(stepConfigurations);
	}

	// @Test
	// void testRunSuccessful() throws IOException {
	// JsonObject data;
	// try (FileReader reader = new
	// FileReader("src/test/resources/xmlstep/data.json")) {
	// data = gson.fromJson(reader, JsonObject.class);
	// }
	// when(context.getTriggerData()).thenReturn(data);
	// StepResult result = xmlStep.run();
	// assertEquals(true, result.isSuccess());
	// assertEquals("XML processed successfully", result.getMessage());
	// }
}

package com.bmg.deliver.workflow.step;

import com.bmg.deliver.enums.AudioTranscodeOption;
import com.bmg.deliver.enums.ProductTranscodeOption;
import com.bmg.deliver.enums.ReleaseType;
import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.enums.AssetOptions;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.bmg.deliver.workflow.execution.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Step {
	private Long id;
	private Workflow workflow;
	private Integer executionOrder;
	private String name;
	private WorkflowStepType type;
	public ExecutionContext context;
	public Logger logger;

	private static final Map<Class<?>, Function<String, Object>> TYPE_PARSERS = new HashMap<>();

	static {
		TYPE_PARSERS.put(Integer.class, Integer::parseInt);
		TYPE_PARSERS.put(Long.class, Long::parseLong);
		TYPE_PARSERS.put(Boolean.class, Boolean::parseBoolean);
		TYPE_PARSERS.put(Double.class, Double::parseDouble);
		TYPE_PARSERS.put(Float.class, Float::parseFloat);
		TYPE_PARSERS.put(String.class, value -> value);
		TYPE_PARSERS.put(ReleaseType.class, ReleaseType::valueOf);
		TYPE_PARSERS.put(ProductTranscodeOption.class, ProductTranscodeOption::valueOf);
		TYPE_PARSERS.put(AudioTranscodeOption.class, AudioTranscodeOption::valueOf);
		TYPE_PARSERS.put(AssetOptions.class, AssetOptions::valueOf);
	}

	protected Step(Long id, Workflow workflow, Integer executionOrder, String name, WorkflowStepType type,
			List<WorkflowStepConfiguration> stepConfigurations) {
		this.id = id;
		this.workflow = workflow;
		this.executionOrder = executionOrder;
		this.name = name;
		this.type = type;

		initConfigurations(stepConfigurations);
	}

	public void initConfigurations(List<WorkflowStepConfiguration> stepConfigurations) {
		for (WorkflowStepConfiguration config : stepConfigurations) {
			String key = config.getKey();
			String value = config.getValue();

			for (Field field : getClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(StepField.class)) {
					StepField stepField = field.getAnnotation(StepField.class);
					if (stepField.key().equals(key)) {
						setFieldValue(field, value);
						break;
					}
				}
			}
		}
	}

	private void setFieldValue(Field field, String value) {
		field.setAccessible(true);
		try {
			Class<?> fieldType = field.getType();
			Function<String, Object> parser = TYPE_PARSERS.get(fieldType);
			if (parser != null) {
				field.set(this, parser.apply(value));
			}
		} catch (Exception e) {
			if (null != logger) {
				logger.error("Error while setting field value: ", e);
			}
		} finally {
			field.setAccessible(false);
		}
	}

	/**
	 * This method is used to execute the step.
	 *
	 * @param context
	 *            - ExecutionContext object containing the workflow instance id,
	 *            trigger data, and other details.
	 * @return StepResult object containing the status of the step and the message.
	 */
	public StepResult execute(ExecutionContext context) {
		this.logger = context.getLogger();
		this.setContext(context);
		try {
			return run();
		} catch (Exception e) {
			logger.error("Exception while execution: ", e);
			return new StepResult(false, e.getMessage());
		}
	}

	/**
	 * This method is used to run the step.
	 *
	 * @return StepResult object containing the status of the step and the message.
	 */
	public abstract StepResult run() throws Exception;
}

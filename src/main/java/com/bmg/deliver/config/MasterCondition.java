package com.bmg.deliver.config;

import com.bmg.deliver.utils.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Slf4j
public class MasterCondition implements Condition {
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String role = context.getEnvironment().getProperty(AppConstants.MASTER_ROLE_ENV);
		return AppConstants.MASTER_ROLE.equals(role);
	}
}

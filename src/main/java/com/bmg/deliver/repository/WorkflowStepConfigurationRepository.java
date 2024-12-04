package com.bmg.deliver.repository;

import com.bmg.deliver.model.WorkflowStepConfiguration;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStepConfigurationRepository extends JpaRepository<WorkflowStepConfiguration, Long> {
	Page<WorkflowStepConfiguration> findByWorkflowStepId(Long id, Pageable pageable);

	WorkflowStepConfiguration findByKey(String key);

	List<WorkflowStepConfiguration> findByWorkflowStepId(Long workflowStepId);

	void deleteAllByWorkflowStepId(Long id);

	WorkflowStepConfiguration findByWorkflowStepIdAndKey(Long workflowStepId, String key);
}

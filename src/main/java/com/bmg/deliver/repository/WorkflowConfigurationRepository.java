package com.bmg.deliver.repository;

import com.bmg.deliver.model.WorkflowConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowConfigurationRepository extends JpaRepository<WorkflowConfiguration, Long> {

	Optional<WorkflowConfiguration> findByWorkflowIdAndKey(Long id, String assetIngestionWaitTime);

	List<WorkflowConfiguration> findByWorkflowId(Long workflowId);

	void deleteAllByWorkflowId(Long workflowId);
}

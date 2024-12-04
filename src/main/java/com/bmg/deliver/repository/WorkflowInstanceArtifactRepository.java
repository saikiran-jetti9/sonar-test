package com.bmg.deliver.repository;

import com.bmg.deliver.model.WorkflowInstanceArtifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceArtifactRepository extends JpaRepository<WorkflowInstanceArtifact, Long> {
	Page<WorkflowInstanceArtifact> findByWorkflowInstanceId(Long instanceId, Pageable pageable);

	void deleteAllByWorkflowStepId(Long id);
}

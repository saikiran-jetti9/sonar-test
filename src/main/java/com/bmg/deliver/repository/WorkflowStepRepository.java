package com.bmg.deliver.repository;

import com.bmg.deliver.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

	List<WorkflowStep> findByWorkflowIdOrderByExecutionOrder(Long id);

	void deleteAllByWorkflowId(Long workflowId);

	List<WorkflowStep> findByWorkflowId(Long id);

	Optional<Object> findByWorkflowIdAndExecutionOrder(Long workflowId, int executionOrder);
}

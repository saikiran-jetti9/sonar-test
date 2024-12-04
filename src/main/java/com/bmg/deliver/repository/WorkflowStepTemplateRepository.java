package com.bmg.deliver.repository;

import com.bmg.deliver.model.WorkflowStep;
import com.bmg.deliver.model.WorkflowStepTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepTemplateRepository extends JpaRepository<WorkflowStepTemplate, Long> {

	List<WorkflowStepTemplate> findByTemplateId(Long id);

	void deleteByTemplateId(Long id);

	List<WorkflowStepTemplate> findByWorkflowStepIdIn(List<WorkflowStep> workflowSteps);

	@Query(value = "SELECT * FROM workflow_step_template wst WHERE wst.workflow_step_id = :workflowStepId", nativeQuery = true)
	Optional<WorkflowStepTemplate> findByWorkflowStepId(@Param("workflowStepId") Long workflowStepId);

	void deleteAllByWorkflowStepId(WorkflowStep id);
}

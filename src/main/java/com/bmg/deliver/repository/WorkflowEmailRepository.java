package com.bmg.deliver.repository;

import com.bmg.deliver.model.WorkflowEmail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEmailRepository extends JpaRepository<WorkflowEmail, Long> {

	List<WorkflowEmail> findByWorkflowId(Long workflowId);
}

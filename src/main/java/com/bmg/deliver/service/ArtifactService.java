package com.bmg.deliver.service;

import com.bmg.deliver.model.WorkflowInstanceArtifact;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArtifactService {
	Page<WorkflowInstanceArtifact> getWorkflowInstanceArtifacts(Long instanceId, Pageable pageable);

	WorkflowInstanceArtifact getArtifactById(Long id);

	InputStreamResource getArtifact(Long id);
}

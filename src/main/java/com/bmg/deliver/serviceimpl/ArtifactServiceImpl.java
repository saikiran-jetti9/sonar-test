package com.bmg.deliver.serviceimpl;

import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.model.WorkflowInstanceArtifact;
import com.bmg.deliver.repository.WorkflowInstanceArtifactRepository;
import com.bmg.deliver.service.ArtifactService;
import com.bmg.deliver.utils.AppConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ArtifactServiceImpl implements ArtifactService {

	@Autowired
	private WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository;

	@Value("${app.dirs.attachments}")
	String attachmentsDir;

	@Override
	public Page<WorkflowInstanceArtifact> getWorkflowInstanceArtifacts(Long instanceId, Pageable pageable) {
		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				Sort.by("created").ascending());

		return workflowInstanceArtifactRepository.findByWorkflowInstanceId(instanceId, sortedPageable);
	}

	@Override
	public InputStreamResource getArtifact(Long id) {
		try {
			WorkflowInstanceArtifact artifact = getArtifactById(id);
			String filePath = attachmentsDir + File.separator + artifact.getUniqueFilename();
			File file = new File(filePath);
			if (!file.exists()) {
				log.error("File not found at path: {}", filePath);
				return null;
			}
			FileInputStream fileInputStream = new FileInputStream(file);
			return new InputStreamResource(fileInputStream);
		} catch (FileNotFoundException e) {
			log.error("File not found for ID: {}", id, e);
			return null;
		} catch (WorkflowInstancesNotFoundException e) {
			log.error("Artifact file not found for ID: {}", id, e);
			return null;
		}
	}

	@Override
	public WorkflowInstanceArtifact getArtifactById(Long id) {
		Optional<WorkflowInstanceArtifact> artifact = workflowInstanceArtifactRepository.findById(id);
		if (artifact.isPresent()) {
			return artifact.get();
		} else {
			throw new WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCE_ARTIFACT_NOT_FOUND);
		}
	}
}

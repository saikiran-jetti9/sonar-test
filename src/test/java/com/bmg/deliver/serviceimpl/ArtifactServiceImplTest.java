package com.bmg.deliver.serviceimpl;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.model.WorkflowInstanceArtifact;
import com.bmg.deliver.repository.WorkflowInstanceArtifactRepository;
import com.bmg.deliver.utils.AppConstants;
import java.io.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceImplTest {

	@Mock
	private WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository;

	@InjectMocks
	private ArtifactServiceImpl artifactService;

	private String attachmentsDir = "testDir";

	@BeforeEach
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(artifactService, "attachmentsDir", attachmentsDir);
	}

	// @Test
	// void testGetWorkflowInstanceArtifacts() {
	// Long instanceId = 1L;
	// Pageable pageable = Pageable.unpaged();
	//
	// List<WorkflowInstanceArtifact> artifactsList = new ArrayList<>();
	// WorkflowInstanceArtifact artifact1 = new WorkflowInstanceArtifact();
	// WorkflowInstanceArtifact artifact2 = new WorkflowInstanceArtifact();
	// artifactsList.add(artifact1);
	// artifactsList.add(artifact2);
	// Page<WorkflowInstanceArtifact> page = new PageImpl<>(artifactsList);
	// when(workflowInstanceArtifactRepository.findByWorkflowInstanceId(instanceId,
	// pageable)).thenReturn(page);
	//
	// Page<WorkflowInstanceArtifact> result =
	// artifactService.getWorkflowInstanceArtifacts(instanceId, pageable);
	//
	// assertEquals(artifactsList.size(), result.getContent().size());
	// }

	@Test
	void testGetArtifactSuccess() throws Exception {
		WorkflowInstanceArtifact artifact = new WorkflowInstanceArtifact();
		artifact.setId(1L);
		artifact.setUniqueFilename("file.json");
		when(workflowInstanceArtifactRepository.findById(anyLong())).thenReturn(Optional.of(artifact));
		File testFile = new File(attachmentsDir + File.separator + "file.json");
		testFile.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(testFile)) {
			writer.write("Test Data");
		}
		InputStreamResource result = artifactService.getArtifact(1L);
		assertNotNull(result);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(result.getInputStream()))) {
			assertEquals("Test Data", reader.readLine());
		}
		testFile.delete();
		testFile.getParentFile().delete();
	}

	@Test
	void testGetArtifactFileNotExists() {
		WorkflowInstanceArtifact artifact = new WorkflowInstanceArtifact();
		artifact.setId(1L);
		artifact.setUniqueFilename("file.json");
		when(workflowInstanceArtifactRepository.findById(anyLong())).thenReturn(Optional.of(artifact));
		InputStreamResource result = artifactService.getArtifact(1L);
		assertNull(result);
	}

	@Test
	void testGetArtifactByIdNotFound() {
		Long id = 1L;
		when(workflowInstanceArtifactRepository.findById(anyLong())).thenReturn(Optional.empty());
		WorkflowInstancesNotFoundException exception = assertThrows(WorkflowInstancesNotFoundException.class,
				() -> artifactService.getArtifactById(id));
		assertEquals(AppConstants.WORKFLOW_INSTANCE_ARTIFACT_NOT_FOUND, exception.getMessage());
	}

	@Test
	void testGetArtifactWorkflowInstancesNotFoundException() {
		Long id = 1L;
		WorkflowInstanceArtifact artifact = new WorkflowInstanceArtifact();
		artifact.setId(id);
		artifact.setUniqueFilename("file.json");
		when(workflowInstanceArtifactRepository.findById(anyLong())).thenReturn(Optional.empty());
		InputStreamResource result = artifactService.getArtifact(1L);
		assertNull(result);
	}
}

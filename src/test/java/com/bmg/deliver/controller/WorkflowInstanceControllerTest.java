package com.bmg.deliver.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.exceptions.WorkflowInstanceIdNotFoundException;
import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.model.WorkflowInstanceArtifact;
import com.bmg.deliver.service.ArtifactService;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.utils.AppConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class WorkflowInstanceControllerTest {

	@Mock
	private WorkflowInstanceService workflowInstanceService;

	@Mock
	private ArtifactService workflowInstanceArtifactService;

	@InjectMocks
	private WorkflowInstanceController workflowInstanceController;

	@Test
	void testGetWorkflowInstance_Success() {
		WorkflowInstance mockInstance = new WorkflowInstance();
		when(workflowInstanceService.getWorkflowInstanceById(1L)).thenReturn(Optional.of(mockInstance));

		Optional<WorkflowInstance> result = workflowInstanceController.getWorkflowInstance(1L);

		assertTrue(result.isPresent());
		assertEquals(mockInstance, result.get());
	}

	@Test
  void testGetWorkflowInstance_NotFound() {
    when(workflowInstanceService.getWorkflowInstanceById(1L))
        .thenThrow(
            new WorkflowInstanceIdNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND));

    assertThrows(
        WorkflowInstanceIdNotFoundException.class,
        () -> {
          workflowInstanceController.getWorkflowInstance(1L);
        });
  }

	@Test
	void testGetWorkflowInstanceArtifacts() {
		List<WorkflowInstanceArtifact> artifacts = new ArrayList<>();
		artifacts.add(new WorkflowInstanceArtifact());
		Page<WorkflowInstanceArtifact> page = new PageImpl<>(artifacts);
		when(workflowInstanceArtifactService.getWorkflowInstanceArtifacts(eq(1L), any(Pageable.class)))
				.thenReturn(page);

		Page<WorkflowInstanceArtifact> result = workflowInstanceController.getWorkflowInstanceArtifacts(1L, 0, 10);

		assertNotNull(result);
		assertEquals(page, result);
	}

	@Test
	void testGetLogs_returnsLogs_whenWorkflowInstanceExists() {
		Long id = 123L;
		String expectedLogs = "Sample logs";
		when(workflowInstanceService.getLogsOfWorkflowInstance(id)).thenReturn(expectedLogs);
		String actualLogs = workflowInstanceController.getLogs(id);
		assertEquals(expectedLogs, actualLogs);
		verify(workflowInstanceService).getLogsOfWorkflowInstance(id);
	}

	@Test
	void testGetLogs_throwsException_whenWorkflowInstanceNotFound() {
		Long id = 123L;

		when(workflowInstanceService.getLogsOfWorkflowInstance(id))
				.thenThrow(new WorkflowInstancesNotFoundException("Workflow instance not found"));

		WorkflowInstancesNotFoundException exception = assertThrows(WorkflowInstancesNotFoundException.class, () -> {
			workflowInstanceController.getLogs(id);
		});
		assertEquals(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id, exception.getMessage());
		verify(workflowInstanceService).getLogsOfWorkflowInstance(id);
	}

	// @Test
	// void testGetWorkflowInstancesSuccess() {
	// Pageable pageable = PageRequest.of(0, 10);
	// Page<WorkflowInstance> workflowInstancesPage = new
	// PageImpl<>(Collections.emptyList(), pageable, 0);
	// when(workflowInstanceService.getAllInstances(pageable,
	// null)).thenReturn(workflowInstancesPage);
	// Page<WorkflowInstance> result =
	// workflowInstanceController.getWorkflowInstances(0, 10, null);
	// assertNotNull(result);
	// }

	@Test
  void testGetWorkflowInstances_WorkflowInstancesNotFoundException() {
    when(workflowInstanceService.getWorkflowInstancesByStatus(
            any(Pageable.class), any(WorkflowInstanceStatus.class)))
        .thenReturn(null);
    WorkflowInstancesNotFoundException exception =
        assertThrows(
            WorkflowInstancesNotFoundException.class,
            () -> {
              workflowInstanceController.getWorkflowInstancesByStatus(0, 10, null);
            });
    assertEquals(AppConstants.ERROR_RETRIEVING_INSTANCES, exception.getMessage());
  }

	// @Test
	// void testUpdateWorkflowInstance() {
	// Long id = 1L;
	// WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
	// workflowInstanceDTO.setStatus(WorkflowInstanceStatus.TERMINATED);
	//
	// WorkflowInstance workflowInstance = new WorkflowInstance();
	// workflowInstance.setId(id);
	// workflowInstance.setStatus(workflowInstanceDTO.getStatus());
	//
	// when(workflowInstanceService.updateWorkflowInstance(id,
	// workflowInstanceDTO)).thenReturn(workflowInstance);
	//
	// when(workflowInstanceService.updateWorkflowInstance(id,
	// workflowInstanceDTO)).thenReturn(workflowInstance);
	//
	// WorkflowInstance result =
	// workflowInstanceController.updateWorkflowInstance(id, workflowInstanceDTO);
	// assertEquals(workflowInstanceDTO.getStatus(), result.getStatus());
	//
	// verify(workflowInstanceService, times(1)).updateWorkflowInstance(id,
	// workflowInstanceDTO);
	//
	// when(workflowInstanceService.updateWorkflowInstance(anyLong(), any()))
	// .thenThrow(new
	// WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND
	// + id));
	//
	// Exception exception = assertThrows(WorkflowInstancesNotFoundException.class,
	// () -> {
	// workflowInstanceController.updateWorkflowInstance(id, workflowInstanceDTO);
	// });
	// assertEquals(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id,
	// exception.getMessage());
	//
	// verify(workflowInstanceService, times(2)).updateWorkflowInstance(id,
	// workflowInstanceDTO);
	// }

	// @Test
	// void testDeleteWorkflowInstance() {
	// Long id = 1L;
	//
	// doNothing().when(workflowInstanceService).deleteWorkflowInstance(id);
	//
	// assertDoesNotThrow(() ->
	// workflowInstanceController.deleteWorkflowInstance(id));
	//
	// verify(workflowInstanceService, times(1)).deleteWorkflowInstance(id);
	//
	// doThrow(new
	// WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND
	// + id))
	// .when(workflowInstanceService).deleteWorkflowInstance(id);
	//
	// Exception exception = assertThrows(WorkflowInstancesNotFoundException.class,
	// () -> {
	// workflowInstanceController.deleteWorkflowInstance(id);
	// });
	// assertEquals(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id,
	// exception.getMessage());
	//
	// verify(workflowInstanceService, times(2)).deleteWorkflowInstance(id);
	// }
}

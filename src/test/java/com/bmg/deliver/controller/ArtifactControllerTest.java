package com.bmg.deliver.controller;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.bmg.deliver.service.ArtifactService;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ArtifactControllerTest {

	@InjectMocks
	private ArtifactController artifactController;

	@Mock
	private ArtifactService artifactService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testDownloadArtifactSuccess() {
		InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream("Sample File".getBytes()));
		when(artifactService.getArtifact(anyLong())).thenReturn(resource);
		ResponseEntity<InputStreamResource> response = artifactController.downloadArtifact(1L);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("attachment;filename=1", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
		assertEquals(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM,
				response.getHeaders().getContentType());
		assertEquals(resource, response.getBody());
	}

	@Test
  void testDownloadArtifactNotFound() {
    when(artifactService.getArtifact(anyLong())).thenReturn(null);
    ResponseEntity<InputStreamResource> response = artifactController.downloadArtifact(1L);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

	@Test
  void testDownloadArtifactException() {
    when(artifactService.getArtifact(anyLong())).thenThrow(new RuntimeException("Test Exception"));
    ResponseEntity<InputStreamResource> response = artifactController.downloadArtifact(1L);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }
}

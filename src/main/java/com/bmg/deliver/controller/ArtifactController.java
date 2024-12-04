package com.bmg.deliver.controller;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.service.ArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Conditional(MasterCondition.class)
@RequestMapping("/api/artifact")
@Slf4j
public class ArtifactController {

	@Autowired
	private ArtifactService artifactService;

	@GetMapping("/{id}")
	@Operation(tags = {
			"Workflow-Instance-Controller"}, description = "Download an artifact file based on Id", summary = "Download artifact file")
	public ResponseEntity<InputStreamResource> downloadArtifact(@PathVariable Long id) {
		log.info("Received request to download artifact with id: {}", id);
		try {
			InputStreamResource resource = artifactService.getArtifact(id);
			if (resource == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + id)
					.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
		} catch (Exception e) {
			log.error("IOException occurred: ", e);
			return ResponseEntity.status(500).build();
		}
	}
}

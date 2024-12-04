package com.bmg.deliver.controller;

import com.bmg.deliver.dto.TemplateDto;
import com.bmg.deliver.dto.WorkflowStepTemplateDTO;
import com.bmg.deliver.exceptions.ApplicationRuntimeException;
import com.bmg.deliver.exceptions.TemplateIdNotFoundException;
import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.model.Template;
import com.bmg.deliver.model.TemplateVersion;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.service.TemplateService;
import com.bmg.deliver.service.WorkflowStepTemplateService;
import com.bmg.deliver.utils.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/template")
@Slf4j
public class TemplateController {

	@Autowired
	TemplateService templateService;

	@Autowired
	WorkflowStepTemplateService workflowStepTemplateService;

	@PostMapping
	@Operation(tags = {"Template-Controller"}, description = "Create a new template", summary = "Create a new template")
	public Template createTemplate(@RequestBody TemplateDto templateDto) {
		try {
			return templateService.createTemplate(templateDto);
		} catch (Exception e) {
			log.error("Error While Creating Template", e);
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/{id}")
	@Operation(tags = {
			"Template-Controller"}, description = "Update an existing template", summary = "Update an existing template")
	public Template updateTemplate(@PathVariable Long id, @RequestBody TemplateDto templateDto) {
		try {
			return templateService.updateTemplate(id, templateDto);
		} catch (TemplateIdNotFoundException e) {
			throw new TemplateIdNotFoundException(AppConstants.TEMPLATE_ID_NOT_FOUND + id);
		} catch (Exception e) {
			log.error("Error While Updating Template", e);
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/{id}")
	@Operation(tags = {
			"Template-Controller"}, description = "Retrieve a specific template", summary = "Retrieve a specific template by ID")
	public Template getTemplate(@PathVariable Long id) {
		try {
			return templateService.getTemplate(id);
		} catch (TemplateIdNotFoundException e) {
			throw new TemplateIdNotFoundException(AppConstants.TEMPLATE_ID_NOT_FOUND + id);
		} catch (Exception e) {
			log.error("Error While Fetching Template", e);
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/{id}")
	@Operation(tags = {
			"Template-Controller"}, description = "Delete a specific template", summary = "Delete a specific template by ID")
	public ResponseEntity<String> deleteTemplate(@PathVariable Long id) {
		try {
			templateService.deleteTemplate(id);
			return ResponseEntity.ok("Template Deleted Successfully with id: " + id);
		} catch (TemplateIdNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AppConstants.TEMPLATE_ID_NOT_FOUND + id);
		} catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AppConstants.DELETED_TEMPLATE_ERROR);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
		}
	}

	@GetMapping
	@Operation(tags = {
			"Template-Controller"}, description = "Retrieve all templates", summary = "Retrieve all templates")
	public Page<Template> getTemplates(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return templateService.getAllTemplates(pageable);

	}

	@GetMapping("/{id}/versions")
	@Operation(tags = {
			"Template-Controller"}, description = "Retrieve all template versions", summary = "Retrieve all template versions")
	public List<TemplateVersion> getTemplateVersions(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @PathVariable Long id) {
		Pageable pageable = PageRequest.of(page, size);
		return templateService.getTemplateVersions(id, pageable);
	}

	@GetMapping("/{id}/usage")
	@Operation(tags = {
			"Template-Controller"}, description = "Retrieve all workflows used by template ", summary = "Retrieve all workflows by templateId")
	public List<Workflow> getListOfWorkflow(@PathVariable Long id) {
		try {
			return templateService.getListWorkflows(id);
		} catch (Exception e) {
			log.error("Error While fetching workflows");
			throw new WorkflowInstancesNotFoundException(AppConstants.ERROR_RETRIEVING_INSTANCES);

		}
	}

	@GetMapping("/workflow/{workflowId}")
	@Operation(tags = {
			"Template-Controller"}, description = "Returns a list of workflow step templates for a specific workflow", summary = "Retrieve workflow step templates")
	public List<WorkflowStepTemplateDTO> getWorkflowStepTemplates(@PathVariable Long workflowId) {
		return workflowStepTemplateService.getWorkflowStepTemplates(workflowId);
	}

}

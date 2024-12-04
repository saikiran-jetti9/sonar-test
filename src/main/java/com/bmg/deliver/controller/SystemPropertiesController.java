package com.bmg.deliver.controller;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.SystemPropertiesDTO;
import com.bmg.deliver.model.SystemProperties;
import com.bmg.deliver.service.SystemPropertiesService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/property")
@Slf4j
@Conditional(MasterCondition.class)
public class SystemPropertiesController {

	@Autowired
	private SystemPropertiesService systemPropertiesService;

	/**
	 * This method is used to add System Properties
	 *
	 * @return ResponseEntity<SystemProperties>
	 */
	@PostMapping
	@Operation(tags = {
			"Workflow-Controller"}, description = "Create a new System Property", summary = "Create a new System Property")
	public ResponseEntity<SystemProperties> addSystemProperty(@RequestBody SystemPropertiesDTO systemPropertiesDTO) {
		try {
			SystemProperties existingProperty = systemPropertiesService
					.getSystemPropertyByKey(systemPropertiesDTO.getKey());
			if (existingProperty != null) {
				SystemProperties updatedProperty = systemPropertiesService
						.updateSystemProperty(existingProperty.getId(), systemPropertiesDTO);
				return new ResponseEntity<>(updatedProperty, HttpStatus.CREATED);

			} else {
				SystemProperties createdProperty = systemPropertiesService.addSystemProperty(systemPropertiesDTO);
				return new ResponseEntity<>(createdProperty, HttpStatus.CREATED);

			}
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * This method is used to get System Property Based on Key
	 *
	 * @return ResponseEntity<SystemProperties>
	 */
	@GetMapping("/{key}")
	@Operation(tags = {
			"Workflow-Controller"}, description = "Retrieve a system property by key", summary = "Get a system property by key")
	public ResponseEntity<SystemProperties> getSystemPropertyByKey(@PathVariable String key) {
		SystemProperties systemProperty = systemPropertiesService.getSystemPropertyByKey(key);

		if (systemProperty == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(systemProperty, HttpStatus.OK);
	}

}

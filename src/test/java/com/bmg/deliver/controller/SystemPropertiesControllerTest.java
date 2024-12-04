package com.bmg.deliver.controller;

import com.bmg.deliver.dto.SystemPropertiesDTO;
import com.bmg.deliver.model.SystemProperties;
import com.bmg.deliver.service.SystemPropertiesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemPropertiesControllerTest {

	@InjectMocks
	private SystemPropertiesController systemPropertiesController;

	@Mock
	private SystemPropertiesService systemPropertiesService;

	SystemPropertiesDTO systemPropertiesDTO = new SystemPropertiesDTO();

	SystemProperties systemProperty = new SystemProperties();

	@BeforeEach
	void setup() {
		systemPropertiesDTO.setKey("key");
		systemPropertiesDTO.setValue("value");
		MockitoAnnotations.openMocks(this);
	}

	@Test
    void testAddSystemPropertySuccess() {
        when(systemPropertiesService.addSystemProperty(any())).thenReturn(systemProperty);
        ResponseEntity<SystemProperties> result = systemPropertiesController.addSystemProperty(systemPropertiesDTO);
        assertEquals(result.getStatusCode(), HttpStatus.CREATED);
    }

	@Test
    void testAddSystemPropertyException() {
        when(systemPropertiesService.addSystemProperty(systemPropertiesDTO)).thenThrow(new RuntimeException("Database error"));
        ResponseEntity<SystemProperties> response = systemPropertiesController.addSystemProperty(systemPropertiesDTO);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

	@Test
	void testGetSystemPropertyByKeySuccess() {
		String key = "someKey";
		when(systemPropertiesService.getSystemPropertyByKey(key)).thenReturn(systemProperty);
		ResponseEntity<SystemProperties> response = systemPropertiesController.getSystemPropertyByKey(key);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(systemProperty, response.getBody());
	}

	@Test
	void testGetSystemPropertyByKeyNotFound() {
		String key = "nonExistentKey";
		when(systemPropertiesService.getSystemPropertyByKey(key)).thenReturn(null);
		ResponseEntity<SystemProperties> response = systemPropertiesController.getSystemPropertyByKey(key);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}
}

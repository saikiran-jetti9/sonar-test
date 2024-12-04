package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.SystemPropertiesDTO;
import com.bmg.deliver.model.SystemProperties;
import com.bmg.deliver.repository.SystemPropertiesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class SystemPropertiesServiceImplTest {

	@InjectMocks
	private SystemPropertiesServiceImpl systemPropertiesService;

	@Mock
	private SystemPropertiesRepository systemPropertiesRepository;

	SystemPropertiesDTO dto = new SystemPropertiesDTO();
	SystemProperties systemProperty = new SystemProperties();

	@BeforeEach
	void setUp() {
		dto.setKey("testKey");
		dto.setValue("testValue");
		dto.setDescription("testDescription");
		systemProperty.setKey(dto.getKey());
		systemProperty.setValue(dto.getValue());
		systemProperty.setDescription(dto.getDescription());
		systemProperty.setId(1L);
		MockitoAnnotations.openMocks(this);
	}

	@Test
    void testAddSystemPropertySuccess() {
        when(systemPropertiesRepository.save(any(SystemProperties.class))).thenReturn(systemProperty);
        SystemProperties result = systemPropertiesService.addSystemProperty(dto);
        assertEquals(systemProperty.getKey(), result.getKey());
        assertEquals(systemProperty.getValue(), result.getValue());
        assertEquals(systemProperty.getDescription(), result.getDescription());
    }

	@Test
	void testAddSystemPropertyNull() {
		SystemProperties result = systemPropertiesService.addSystemProperty(dto);
		assertEquals(null, result);
	}

	@Test
	void testUpdateSystemPropertySuccess() {
		Long id = 1L;

		SystemPropertiesDTO dto = new SystemPropertiesDTO();
		dto.setKey("updatedKey");
		dto.setValue("updatedValue");
		dto.setDescription("updatedDescription");

		SystemProperties existingProperty = new SystemProperties();
		existingProperty.setKey("oldKey");
		existingProperty.setValue("oldValue");
		existingProperty.setDescription("oldDescription");

		SystemProperties updatedProperty = new SystemProperties();
		updatedProperty.setKey(dto.getKey());
		updatedProperty.setValue(dto.getValue());
		updatedProperty.setDescription(dto.getDescription());

		when(systemPropertiesRepository.findById(anyLong())).thenReturn(Optional.of(existingProperty));
		when(systemPropertiesRepository.save(any(SystemProperties.class))).thenReturn(updatedProperty);

		SystemProperties result = systemPropertiesService.updateSystemProperty(id, dto);

		assertEquals(updatedProperty.getKey(), result.getKey());
		assertEquals(updatedProperty.getValue(), result.getValue());
		assertEquals(updatedProperty.getDescription(), result.getDescription());
	}

	@Test
	void testUpdateSystemPropertyNotFound() {
		Long id = 1L;
		SystemProperties result = systemPropertiesService.updateSystemProperty(id, dto);
		assertEquals(null, result);
	}

	@Test
	void testGetSystemPropertyByKeySuccess() {
		String key = "testKey";
		when(systemPropertiesRepository.findByKey(key)).thenReturn(Optional.of(systemProperty));

		SystemProperties result = systemPropertiesService.getSystemPropertyByKey(key);

		assertEquals(systemProperty.getKey(), result.getKey());
		assertEquals(systemProperty.getValue(), result.getValue());
		assertEquals(systemProperty.getDescription(), result.getDescription());
	}

	@Test
	void testGetSystemPropertyByKeyNotFound() {
		String key = "nonExistingKey";
		SystemProperties result = systemPropertiesService.getSystemPropertyByKey(key);
		assertNull(result);
	}

}

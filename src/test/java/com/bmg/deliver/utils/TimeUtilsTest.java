package com.bmg.deliver.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {

	@Test
	void testConvertTimeToMillis_Seconds() {
		assertEquals(2000L, TimeUtils.convertTimeToMillis("2s"));
	}

	@Test
	void testConvertTimeToMillis_Minutes() {
		assertEquals(120000L, TimeUtils.convertTimeToMillis("2m"));
	}

	@Test
	void testConvertTimeToMillis_Hours() {
		assertEquals(7200000L, TimeUtils.convertTimeToMillis("2h"));
	}

	@Test
	void testConvertTimeToMillis_Days() {
		assertEquals(172800000L, TimeUtils.convertTimeToMillis("2d"));
	}

	@Test
	void testConvertTimeToMillis_SingleDigit() {
		assertEquals(1000L, TimeUtils.convertTimeToMillis("1s"));
		assertEquals(60000L, TimeUtils.convertTimeToMillis("1m"));
		assertEquals(3600000L, TimeUtils.convertTimeToMillis("1h"));
		assertEquals(86400000L, TimeUtils.convertTimeToMillis("1d"));
	}

	@Test
	void testConvertTimeToMillis_InvalidUnit() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			TimeUtils.convertTimeToMillis("5x");
		});

		assertEquals("Unsupported time unit: x", exception.getMessage());
	}

	@Test
	void testConvertTimeToMillis_EmptyString() {
		Exception exception = assertThrows(StringIndexOutOfBoundsException.class, () -> {
			TimeUtils.convertTimeToMillis("");
		});

		assertTrue(exception instanceof StringIndexOutOfBoundsException);
	}

}

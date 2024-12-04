package com.bmg.deliver.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TimeUtils {
	public static long convertTimeToMillis(String time) {
		long result;
		char unit = Character.toLowerCase(time.charAt(time.length() - 1));
		long value = Long.parseLong(time.substring(0, time.length() - 1));

		result = switch (unit) {
			case 's' -> value * 1000L;
			case 'm' -> value * 60 * 1000L;
			case 'h' -> value * 60 * 60 * 1000L;
			case 'd' -> value * 24 * 60 * 60 * 1000L;
			default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
		};
		return result;
	}

	public static String subtractDaysFromDate(String dateStr, int numOfDays) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_YYYY_MM_DD);
		LocalDate date = LocalDate.parse(dateStr, formatter);

		LocalDate resultDate = date.minusDays(numOfDays);
		return resultDate.format(formatter);
	}
}

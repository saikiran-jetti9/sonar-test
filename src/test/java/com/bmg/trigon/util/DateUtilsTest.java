package com.bmg.trigon.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DateUtilsTest {

  /**
   * Scenario: Convert a valid date-time string to LocalDateTime object. Expected: The string
   * "2023-09-28 14:30:00" should be converted correctly to LocalDateTime object representing the
   * same date and time.
   */
  @Test
  public void testConvertStringToLocalDateTime_ValidInput() {
    String dateTimeString = "2023-09-28 14:30:00";
    LocalDateTime expectedDate = LocalDateTime.of(2023, 9, 28, 14, 30, 0);

    LocalDateTime result = DateUtils.convertStringToLocalDateTime(dateTimeString);

    assertEquals(expectedDate, result, "The conversion of valid string to LocalDateTime failed.");
  }

  /**
   * Scenario: Try converting an invalid date-time string (wrong format) to LocalDateTime. Expected:
   * The method should throw a DateTimeParseException due to the incorrect format.
   */
  @Test
  public void testConvertStringToLocalDateTime_InvalidFormat() {
    String invalidDateTimeString = "28-09-2023 14:30:00"; // Invalid format

    assertThrows(
        java.time.format.DateTimeParseException.class,
        () -> {
          DateUtils.convertStringToLocalDateTime(invalidDateTimeString);
        },
        "Expected DateTimeParseException for invalid date format.");
  }

  /**
   * Scenario: Format a valid LocalDateTime object to string. Expected: The LocalDateTime
   * "2024-01-01 10:15:30" should be formatted correctly into the string "2024-01-01 10:15:30".
   */
  @Test
  public void testFormatLocalDateTime_ValidInput() {
    LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 10, 15, 30);
    String expectedFormattedString = "2024-01-01 10:15:30";

    String result = DateUtils.formatLocalDateTime(dateTime);

    assertEquals(
        expectedFormattedString, result, "The formatting of LocalDateTime to string failed.");
  }

  /**
   * Scenario: Pass a null LocalDateTime to format method. Expected: The method should throw a
   * NullPointerException since null input is not valid.
   */
  @Test
  public void testFormatLocalDateTime_NullInput() {
    assertThrows(
        NullPointerException.class,
        () -> {
          DateUtils.formatLocalDateTime(null);
        },
        "Expected NullPointerException when formatting null LocalDateTime.");
  }

  /**
   * Scenario: Calculate the difference in days between two LocalDateTime objects representing the
   * same date. Expected: The difference should be 0 days since both objects represent the same day.
   */
  @Test
  public void testCalculateDaysDifference_SameDate() {
    LocalDateTime date1 = LocalDateTime.of(2023, 9, 28, 10, 0, 0);
    LocalDateTime date2 = LocalDateTime.of(2023, 9, 28, 14, 0, 0);

    long result = DateUtils.calculateDaysDifference(date1, date2);

    assertEquals(0, result, "The difference between the same dates should be 0 days.");
  }

  /**
   * Scenario: Calculate the difference in days between two different dates (4 days apart).
   * Expected: The difference should be exactly 4 days.
   */
  @Test
  public void testCalculateDaysDifference_DifferentDates() {
    LocalDateTime date1 = LocalDateTime.of(2023, 9, 28, 10, 0, 0);
    LocalDateTime date2 = LocalDateTime.of(2023, 10, 2, 10, 0, 0); // 4 days difference

    long result = DateUtils.calculateDaysDifference(date1, date2);

    assertEquals(4, result, "The difference between the dates should be 4 days.");
  }

  /**
   * Scenario: Calculate the difference in days between two dates around a leap year. Expected: The
   * difference should account for the leap year and be 2 days (Feb 28 to Mar 1, 2020).
   */
  @Test
  public void testCalculateDaysDifference_LeapYear() {
    LocalDateTime date1 = LocalDateTime.of(2020, 2, 28, 0, 0, 0); // Leap year
    LocalDateTime date2 = LocalDateTime.of(2020, 3, 1, 0, 0, 0); // 2 days apart

    long result = DateUtils.calculateDaysDifference(date1, date2);

    assertEquals(2, result, "The difference should account for leap years and be 2 days.");
  }
}

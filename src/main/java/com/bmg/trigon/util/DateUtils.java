package com.bmg.trigon.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

  public static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Converts a string to a LocalDateTime object using the defined formatter.
   *
   * @param dateTimeString the string representing a date and time
   * @return LocalDateTime object
   */
  public static LocalDateTime convertStringToLocalDateTime(String dateTimeString) {
    return LocalDateTime.parse(dateTimeString, FORMATTER);
  }

  /**
   * Formats a LocalDateTime object into a string using the defined formatter.
   *
   * @param localDateTime the LocalDateTime object to format
   * @return formatted date and time string
   */
  public static String formatLocalDateTime(LocalDateTime localDateTime) {
    return localDateTime.format(FORMATTER);
  }

  /**
   * Calculates the difference in days between two LocalDateTime objects.
   *
   * @param date1 the first date
   * @param date2 the second date
   * @return the absolute difference in days between the two dates
   */
  public static long calculateDaysDifference(LocalDateTime date1, LocalDateTime date2) {
    return Math.abs(ChronoUnit.DAYS.between(date1.toLocalDate(), date2.toLocalDate()));
  }
}

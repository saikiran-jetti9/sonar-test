package com.bmg.trigon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.trigon.common.enums.Territory;
import com.google.cloud.bigquery.*;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BigqueryServiceTest {

  @Mock private BigQuery bigQuery;

  @Mock private TableResult tableResult;

  @InjectMocks private BigqueryService bigqueryService;

  /**
   * Test for getLastRoyaltyRunNumberForPublishing method. Scenario: Successfully retrieves the last
   * royalty run number.
   */
  @Test
  void getLastRoyaltyRunNumberForPublishing_shouldReturnRunNumber() throws InterruptedException {
    Territory territory = Territory.UK;
    long expectedRunNumber = 12345L;

    FieldValueList fieldValueList = mock(FieldValueList.class);
    FieldValue fieldValue = mock(FieldValue.class);

    when(fieldValue.getLongValue()).thenReturn(expectedRunNumber);
    when(fieldValueList.get(0)).thenReturn(fieldValue);
    when(tableResult.iterateAll()).thenReturn(Collections.singletonList(fieldValueList));

    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);

    Long actualRunNumber = bigqueryService.getLastRoyaltyRunNumberForPublishing(territory);

    assertEquals(expectedRunNumber, actualRunNumber);
  }

  /**
   * Test for getLastRoyaltyRunDateForRecorded method. Scenario: Successfully retrieves the last
   * royalty run date.
   */
  @Test
  void getLastRoyaltyRunDateForRecorded_shouldReturnRunDate() throws InterruptedException {
    Territory territory = Territory.BGUK;
    String expectedRunDate = "2024-04-13 12:27:07";

    FieldValueList fieldValueList = mock(FieldValueList.class);
    FieldValue fieldValue = mock(FieldValue.class);

    when(fieldValue.getStringValue()).thenReturn(expectedRunDate);
    when(fieldValueList.get(0)).thenReturn(fieldValue);
    when(tableResult.iterateAll()).thenReturn(Collections.singletonList(fieldValueList));

    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);

    String actualRunDate = bigqueryService.getLastRoyaltyRunDateForRecorded(territory);

    assertEquals(expectedRunDate, actualRunDate);
  }
}

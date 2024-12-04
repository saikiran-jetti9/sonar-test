package com.bmg.trigon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.trigon.common.enums.Territory;
import com.bmg.trigon.model.SAPMasterDataSync;
import com.bmg.trigon.repository.SAPMasterDataSyncRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SapDataSyncTerritorySchedulerTest {

  @Mock private MasterDataSyncService masterDataSyncService;

  @Mock private BigqueryService bigqueryService;

  @Mock private SAPMasterDataSyncRepository sapMasterDataSyncRepository;

  @InjectMocks private SapDataSyncTerritoryScheduler sapDataSyncTerritoryScheduler;

  @BeforeEach
  void setUp() {
    // Use ReflectionTestUtils to set the value of the 'maximumDays' field
    ReflectionTestUtils.setField(sapDataSyncTerritoryScheduler, "maximumDays", 30);
    ReflectionTestUtils.setField(sapDataSyncTerritoryScheduler, "mdSyncEnable", true);
  }

  @Test
  void testExecuteTask_forPublishingTerritory_royaltyRunHappened() throws InterruptedException {

    Territory territory = Territory.GE;
    when(bigqueryService.getLastRoyaltyRunNumberForPublishing(territory)).thenReturn(118L);
    when(sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name()))
        .thenReturn(Optional.empty());

    sapDataSyncTerritoryScheduler.executeTask(territory);

    verify(masterDataSyncService, times(1)).processAllVendors(territory);
    verify(sapMasterDataSyncRepository, times(2)).save(any(SAPMasterDataSync.class));
  }

  @Test
  void testExecuteTask_forPublishingTerritory_noRoyaltyRun() throws InterruptedException {
    Territory territory = Territory.GE;
    when(bigqueryService.getLastRoyaltyRunNumberForPublishing(territory)).thenReturn(null);

    sapDataSyncTerritoryScheduler.executeTask(territory);

    verify(masterDataSyncService, never()).processAllVendors(any());
  }

  @Test
  void testCheckIfRecordingRoyaltyRunHappend_noPreviousData() throws InterruptedException {
    Territory territory = Territory.BGDE;
    when(bigqueryService.getLastRoyaltyRunDateForRecorded(territory))
        .thenReturn("2024-04-13 11:46:51");
    when(sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name()))
        .thenReturn(Optional.empty());

    String lastRoyaltyRunDate =
        sapDataSyncTerritoryScheduler.checkIfRecordingRoyaltyRunHappenedAndReturnLastRoyaltyRunDate(
            territory);

    assertNotNull(lastRoyaltyRunDate);
  }

  @Test
  void testCheckIfRecordingRoyaltyRunHappend_existingData() throws InterruptedException {

    Territory territory = Territory.BGDE;
    when(bigqueryService.getLastRoyaltyRunDateForRecorded(territory))
        .thenReturn("2024-04-13 11:46:51");
    SAPMasterDataSync existingSync = new SAPMasterDataSync();
    existingSync.setSyncStartedAt("2024-04-01 11:46:51");
    existingSync.setLastRunDate("2024-04-12 11:46:51");
    existingSync.setSyncFinishedAt("");
    when(sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name()))
        .thenReturn(Optional.of(existingSync));

    String lastRoyaltyRunDate =
        sapDataSyncTerritoryScheduler.checkIfRecordingRoyaltyRunHappenedAndReturnLastRoyaltyRunDate(
            territory);

    assertNotNull(lastRoyaltyRunDate);
  }

  @Test
  void testCheckIfRecordingRoyaltyRunHappend_NewRoyaltyRun() throws InterruptedException {
    Territory territory = Territory.BGDE;
    when(bigqueryService.getLastRoyaltyRunDateForRecorded(territory))
        .thenReturn("2024-04-13 11:46:51");
    when(sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name()))
        .thenReturn(Optional.empty());

    String lastRoyaltyRunDate =
        sapDataSyncTerritoryScheduler.checkIfRecordingRoyaltyRunHappenedAndReturnLastRoyaltyRunDate(
            territory);

    assertNotNull(lastRoyaltyRunDate);
  }

  @Test
  void testCheckIfPublishingRoyaltyRunHappend_noPreviousData() throws InterruptedException {
    Territory territory = Territory.GE;
    when(bigqueryService.getLastRoyaltyRunNumberForPublishing(territory)).thenReturn(118L);
    when(sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name()))
        .thenReturn(Optional.empty());

    Long lastRoyaltyRunNumber =
        sapDataSyncTerritoryScheduler.checkIfPublishingRoyaltyRunHappenedAndReturnRoyaltyNumber(
            territory);

    assertNotNull(lastRoyaltyRunNumber);
  }

  @Test
  void testCheckIfPublishingRoyaltyRunHappend_existingData() throws InterruptedException {

    Territory territory = Territory.UK;
    when(bigqueryService.getLastRoyaltyRunNumberForPublishing(territory)).thenReturn(120L);
    SAPMasterDataSync existingSync = new SAPMasterDataSync();
    existingSync.setSyncStartedAt("2024-04-01 11:46:51");
    existingSync.setLastRunNumber(118L);
    existingSync.setSyncFinishedAt("");
    when(sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name()))
        .thenReturn(Optional.of(existingSync));

    Long lastRoyaltyRunNumber =
        sapDataSyncTerritoryScheduler.checkIfPublishingRoyaltyRunHappenedAndReturnRoyaltyNumber(
            territory);

    assertNotNull(lastRoyaltyRunNumber);
  }

  @Test
  void testCheckIfPublishingRoyaltyRunHappend_NewRoyaltyRun() throws InterruptedException {
    Territory territory = Territory.UK;
    when(bigqueryService.getLastRoyaltyRunNumberForPublishing(territory)).thenReturn(118L);
    when(sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name()))
        .thenReturn(Optional.empty());

    Long lastRoyaltyRunNumber =
        sapDataSyncTerritoryScheduler.checkIfPublishingRoyaltyRunHappenedAndReturnRoyaltyNumber(
            territory);

    assertNotNull(lastRoyaltyRunNumber);
  }

  /**
   * Test when daysDifference is greater than maximumDays and syncFinishedAt is null (sync should
   * happen).
   */
  @Test
  void calculateDaysDifferenceAndCheckSyncFinishedDate_shouldSync_dueToDaysDifference() {
    LocalDateTime syncStartedAt = LocalDateTime.now().minusDays(31);
    String syncFinishedAt = "2024-09-20 12:00:00";

    boolean result =
        sapDataSyncTerritoryScheduler.calculateDaysDifferenceAndCheckSyncFinishedDate(
            syncStartedAt, syncFinishedAt);

    assertTrue(result);
  }

  /** Test when syncFinishedAt is empty (sync should happen). */
  @Test
  void calculateDaysDifferenceAndCheckSyncFinishedDate_shouldSync_dueToEmptySyncFinishedAt() {
    LocalDateTime syncStartedAt = LocalDateTime.now().minusDays(5);
    String syncFinishedAt = "";

    boolean result =
        sapDataSyncTerritoryScheduler.calculateDaysDifferenceAndCheckSyncFinishedDate(
            syncStartedAt, syncFinishedAt);

    assertTrue(result);
  }

  /**
   * Test when daysDifference is less than or equal to maximumDays and syncFinishedAt is not empty
   * (sync should not happen).
   */
  @Test
  void calculateDaysDifferenceAndCheckSyncFinishedDate_shouldNotSync() {
    LocalDateTime syncStartedAt = LocalDateTime.now().minusDays(5);
    String syncFinishedAt = "2024-09-20 12:00:00";
    boolean result =
        sapDataSyncTerritoryScheduler.calculateDaysDifferenceAndCheckSyncFinishedDate(
            syncStartedAt, syncFinishedAt);

    assertFalse(result);
  }

  /**
   * Test when daysDifference is equal to maximumDays and syncFinishedAt is empty (sync should
   * happen).
   */
  @Test
  void
      calculateDaysDifferenceAndCheckSyncFinishedDate_shouldSync_dueToEqualDaysDifferenceAndEmptySyncFinishedAt() {
    LocalDateTime syncStartedAt = LocalDateTime.now().minusDays(10);
    String syncFinishedAt = "";

    boolean result =
        sapDataSyncTerritoryScheduler.calculateDaysDifferenceAndCheckSyncFinishedDate(
            syncStartedAt, syncFinishedAt);

    assertTrue(result);
  }
}

package com.bmg.trigon.service;

import com.bmg.trigon.common.enums.Territory;
import com.bmg.trigon.model.SAPMasterDataSync;
import com.bmg.trigon.model.Segment;
import com.bmg.trigon.repository.SAPMasterDataSyncRepository;
import com.bmg.trigon.util.DateUtils;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SapDataSyncTerritoryScheduler {

  /** The time when the SAP MD trigger should be executed. */
  @Value("${com.bmg.sap.md.scheduling.sap-md-trigger-time}")
  private String sapMdTriggerTime;

  /** maximum days to wait to sync the Master Data */
  @Value("${com.bmg.sap.md.scheduling.maximum-days-to-wait}")
  private int maximumDays;

  /** If mdSyncEnable is true : MD sync is enabled, false : MD sync is disabled */
  @Value("${com.bmg.sap.md.scheduling.md-sync-enable}")
  private boolean mdSyncEnable;

  private final MasterDataSyncService masterDataSyncService;

  private final BigqueryService bigqueryService;

  private final SAPMasterDataSyncRepository sapMasterDataSyncRepository;

  /** The task scheduler to use for scheduling tasks. */
  private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

  private static final int POOL_SIZE = 5;

  public SapDataSyncTerritoryScheduler(
      MasterDataSyncService masterDataSyncService,
      BigqueryService bigqueryService,
      SAPMasterDataSyncRepository sapMasterDataSyncRepository) {
    this.masterDataSyncService = masterDataSyncService;
    this.bigqueryService = bigqueryService;
    this.sapMasterDataSyncRepository = sapMasterDataSyncRepository;
  }

  /** Initializes the task scheduler and schedules the tasks. */
  @PostConstruct
  public void init() {
    taskScheduler.setPoolSize(POOL_SIZE);
    taskScheduler.initialize();
    scheduleTasks();
  }

  /** Schedules tasks for all territories. */
  private void scheduleTasks() {
    // Get the time zone map
    Map<Territory, String> timeZoneMap = Territory.getTimeZoneMap();
    for (Map.Entry<Territory, String> entry : timeZoneMap.entrySet()) {
      Territory territory = entry.getKey();
      String zone = entry.getValue();
      if (territory != null && zone != null) {
        scheduleTask(territory, zone);
      }
    }
  }

  /**
   * Schedules a task for the given territory.
   *
   * @param territory the territory for which to schedule the task
   * @param zone the time zone for the territory
   */
  private void scheduleTask(Territory territory, String zone) {
    taskScheduler.schedule(() -> executeTask(territory), cronTrigger(sapMdTriggerTime, zone));
  }

  /**
   * Executes the task for the given territory.
   *
   * @param cron the cron expression for the trigger
   * @param zone the time zone for the trigger
   * @return the cron trigger
   */
  private CronTrigger cronTrigger(String cron, String zone) {
    return new CronTrigger(cron, TimeZone.getTimeZone(zone));
  }

  /**
   * Executes the task for the given territory.
   *
   * @param territory the territory for which to execute the task
   */
  public void executeTask(Territory territory) {

    if (!mdSyncEnable) {
      log.info("Master Data synchronization is disabled by configuration. Skipping execution.");
      return;
    }

    log.info(
        "Configured MD sync time in configuration (in {} territory timezone): {}",
        territory.name(),
        ZonedDateTime.now(territory.getZoneId()));
    log.info("Beginning scheduled task execution for territory: {}", territory.name());
    try {

      // Get publishing and recording territories which are available in SAP MD data
      Set<Territory> territories = getPublishingAndRecordingTerritories(territory);
      log.info("Recording and publishing territories for {} are: {}", territory, territories);

      // Iterate over each retrieved territory to process accordingly
      for (Territory territory1 : territories) {
        if (territory1.isPublishing()) {
          log.info(
              "Processing publishing territory: {}. Preparing to check royalty run.",
              territory1.name());

          // Check if a publishing royalty run has happened
          Long royaltyRunNumber =
              checkIfPublishingRoyaltyRunHappenedAndReturnRoyaltyNumber(territory1);

          // If a royalty run happened, sync master data for all vendors and
          // store the SAPMasterDataSync record in the DB.
          if (royaltyRunNumber != null) {
            // prepare new record for with new sync details
            String newSyncStartedAtString = DateUtils.formatLocalDateTime(LocalDateTime.now());
            SAPMasterDataSync newMasterDataSync = new SAPMasterDataSync();
            newMasterDataSync.setTerritory(territory1.name());
            newMasterDataSync.setSegment(Segment.PUBLISHING.name());
            newMasterDataSync.setLastRunNumber(royaltyRunNumber);
            newMasterDataSync.setSyncStartedAt(newSyncStartedAtString);
            // save the record before processing
            sapMasterDataSyncRepository.save(newMasterDataSync);
            log.info("Sync task started for territory {}", territory.name());

            // process the vendors
            masterDataSyncService.processAllVendors(territory1);

            // If synced successfully, set the syncFinishedAt and save the record
            String syncFinishedAt = DateUtils.formatLocalDateTime(LocalDateTime.now());
            newMasterDataSync.setSyncFinishedAt(syncFinishedAt);

            // save the record after processing
            sapMasterDataSyncRepository.save(newMasterDataSync);
            log.info(
                "Sync completed for {}. Started: {}, Finished: {}",
                territory1.name(),
                newSyncStartedAtString,
                syncFinishedAt);
          } else {
            log.info(
                "No new publishing royalty run identified for territory: {}.", territory1.name());
          }
        } else if (territory1.isRecording()) {
          log.info(
              "Processing recording territory: {}. Preparing to check royalty run.",
              territory1.name());
          // Check if a recording royalty run has happened
          String lastRoyaltyRunDate =
              checkIfRecordingRoyaltyRunHappenedAndReturnLastRoyaltyRunDate(territory1);

          // If a royalty run happened, sync master data for all vendors and
          // store the SAPMasterDataSync record in the DB.
          if (lastRoyaltyRunDate != null) {
            // prepare new record for new sync
            String newSyncStartedAtString = DateUtils.formatLocalDateTime(LocalDateTime.now());
            SAPMasterDataSync newMasterDataSync = new SAPMasterDataSync();
            newMasterDataSync.setTerritory(territory1.name());
            newMasterDataSync.setSegment(Segment.PUBLISHING.name());
            newMasterDataSync.setLastRunDate(lastRoyaltyRunDate);
            newMasterDataSync.setSyncStartedAt(newSyncStartedAtString);
            // save the record before processing
            sapMasterDataSyncRepository.save(newMasterDataSync);
            log.info("Sync task started for territory {}", territory.name());

            // process the vendors
            masterDataSyncService.processAllVendors(territory1);

            // If synced successfully, set the syncFinishedAt and save the record
            String syncFinishedAt = DateUtils.formatLocalDateTime(LocalDateTime.now());
            newMasterDataSync.setSyncFinishedAt(syncFinishedAt);

            // save the record after processing
            sapMasterDataSyncRepository.save(newMasterDataSync);
            log.info(
                "Sync completed for {}. Started: {}, Finished: {}",
                territory1.name(),
                newSyncStartedAtString,
                syncFinishedAt);
          } else {
            log.info(
                "No new publishing royalty run identified for territory: {}.", territory1.name());
          }
        }
      }
    } catch (Exception e) {
      log.error(
          "Error during MD sync execution for territory: {}: {}.",
          territory.name(),
          e.getMessage(),
          e);
    }
  }

  public Set<Territory> getPublishingAndRecordingTerritories(Territory territory) {
    return Stream.concat(
            Territory.getPublishingTerritory(territory).stream(),
            Stream.of(Territory.getRecordingTerritory(territory)))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * Checks if a royalty run has occurred for a recording territory by comparing the last royalty
   * run date in BigQuery and the DB.
   *
   * @param territory the recording territory
   * @return lastRoyaltyRunDateFromBQString if a royalty run has occurred, null otherwise
   */
  public String checkIfRecordingRoyaltyRunHappenedAndReturnLastRoyaltyRunDate(Territory territory) {
    log.info(
        "Checking if a recording royalty run has happened for territory: {}.", territory.name());
    // Get the last royalty run date for the recorded territory
    try {
      // Example : 2024-04-13 11:46:51
      String latestRoyaltyRunDateFromBQString =
          bigqueryService.getLastRoyaltyRunDateForRecorded(territory);
      log.info(
          "Latest royalty run date from Bigquery for {} is {}",
          territory,
          latestRoyaltyRunDateFromBQString);

      // If no run date is retrieved, return null
      if (StringUtils.isEmpty(latestRoyaltyRunDateFromBQString)) {
        return null;
      }

      // Get the corresponding sync data from the repository
      Optional<SAPMasterDataSync> dataSyncOptional =
          sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name());

      // If data is not present, then it is the first time syncing that territory
      if (dataSyncOptional.isEmpty()) {
        log.info(
            "This is the first synchronization attempt for the {} territory.", territory.name());
        return latestRoyaltyRunDateFromBQString;
      }

      SAPMasterDataSync masterDataSync = dataSyncOptional.get();
      LocalDateTime syncStartedAt =
          DateUtils.convertStringToLocalDateTime(masterDataSync.getSyncStartedAt());
      LocalDateTime lastRunDateFromDB =
          DateUtils.convertStringToLocalDateTime(masterDataSync.getLastRunDate());
      LocalDateTime latestRoyaltyRunDateFromBQ =
          DateUtils.convertStringToLocalDateTime(latestRoyaltyRunDateFromBQString);

      // If the run date from BigQuery is after or equal to the one in DB, check other conditions
      if (latestRoyaltyRunDateFromBQ.isAfter(lastRunDateFromDB)
          || latestRoyaltyRunDateFromBQ.isEqual(lastRunDateFromDB)) {
        log.info(
            "For {} territory, latest royalty run date from Bigquery is : {} and last sync royalty run date is : {}",
            territory.name(),
            latestRoyaltyRunDateFromBQString,
            masterDataSync.getLastRunDate());
        // calculate the days between last sync started at and current date
        // If the days difference more than maximumDays
        // or sync finished date is null or empty
        // then we have to sync the master data for that particular territory
        String syncFinishedAt = masterDataSync.getSyncFinishedAt();
        boolean shouldRoyaltyRunHappen =
            calculateDaysDifferenceAndCheckSyncFinishedDate(syncStartedAt, syncFinishedAt);
        if (shouldRoyaltyRunHappen) {

          // logging the reason for why the royalty run should happen
          if (StringUtils.isEmpty(syncFinishedAt)) {
            log.info(
                "Retrying sync for territory: {} due to incomplete previous run", territory.name());
          } else {
            log.info(
                "Days difference : {} is more then configured days :{}. Syncing process initiated for {} territory",
                DateUtils.calculateDaysDifference(LocalDateTime.now(), syncStartedAt),
                maximumDays,
                territory.name());
          }
          return latestRoyaltyRunDateFromBQString;
        } else {
          log.info(
              "Skipping the MD sync due to daysDifference: {} is not more than configured days: {} for {} territory",
              DateUtils.calculateDaysDifference(LocalDateTime.now(), syncStartedAt),
              maximumDays,
              territory.name());
        }
      }
      return null;
    } catch (InterruptedException e) {
      log.error(
          "Error checking recording royalty run for territory: {}: {}",
          territory.name(),
          e.getMessage(),
          e);
      return null;
    }
  }

  public boolean calculateDaysDifferenceAndCheckSyncFinishedDate(
      LocalDateTime syncStartedAt, String syncFinishedAt) {
    long daysDifference = DateUtils.calculateDaysDifference(LocalDateTime.now(), syncStartedAt);
    log.info(
        "Days difference between lastSyncStartedAt: {} and current date: {} is : {}",
        syncStartedAt,
        LocalDateTime.now(),
        daysDifference);
    log.info("last SyncFinishedAt date is : {}", syncFinishedAt);
    return daysDifference > maximumDays || StringUtils.isEmpty(syncFinishedAt);
  }

  /**
   * Checks if a royalty run has occurred for a publishing territory by comparing the last royalty
   * run number in BigQuery and the DB.
   *
   * @param territory the publishing territory
   * @return lastRoyaltyRunNumberFromBQ if a royalty run has occurred, null otherwise
   */
  public Long checkIfPublishingRoyaltyRunHappenedAndReturnRoyaltyNumber(Territory territory) {
    log.info(
        "Checking if a publishing royalty run has happened for territory: {}.", territory.name());
    // Get the latest royalty run number for the publishing territory
    try {
      // Example : 118
      Long latestRoyaltyRunNumberFromBQ =
          bigqueryService.getLastRoyaltyRunNumberForPublishing(territory);
      log.info(
          "Latest royalty run number from Bigquery for {} is {}",
          territory,
          latestRoyaltyRunNumberFromBQ);

      // If no run number is retrieved, return null
      if (latestRoyaltyRunNumberFromBQ == null) {
        return null;
      }

      // Get the corresponding sync data from the repository
      Optional<SAPMasterDataSync> dataSyncOptional =
          sapMasterDataSyncRepository.findTopByTerritoryOrderBySyncStartedAtDesc(territory.name());

      // If no previous sync data exists, this is the first sync for the territory
      if (dataSyncOptional.isEmpty()) {
        log.info(
            "This is the first synchronization attempt for the {} territory.", territory.name());
        return latestRoyaltyRunNumberFromBQ;
      }

      SAPMasterDataSync masterDataSync = dataSyncOptional.get();
      Long lastRoyaltyRunNumberFromDB = masterDataSync.getLastRunNumber();
      LocalDateTime syncStartedAt =
          DateUtils.convertStringToLocalDateTime(masterDataSync.getSyncStartedAt());

      // If the run number from BigQuery is not equal to the one in DB, check other
      // conditions
      if (!Objects.equals(latestRoyaltyRunNumberFromBQ, lastRoyaltyRunNumberFromDB)) {
        log.info("Royalty run has happened for recording territory: {}", territory.name());
        // calculate the days between last sync started at and current date
        // If the days difference more than maximumDays
        // then we have to sync the master data for that particular territory
        long daysDifference = DateUtils.calculateDaysDifference(LocalDateTime.now(), syncStartedAt);
        log.info(
            "Days difference between lastSyncStartedAt: {} and current time: {} is : {}",
            syncStartedAt,
            LocalDateTime.now(),
            daysDifference);
        if (daysDifference > maximumDays) {
          log.info(
              "Days difference : {} is more then configured days :{}. Syncing process initiated for {} territory",
              daysDifference,
              maximumDays,
              territory.name());
          return latestRoyaltyRunNumberFromBQ;
        } else {
          log.info(
              "Skipping the MD sync due to daysDifference: {} is not more than configured days: {} for {} territory",
              daysDifference,
              maximumDays,
              territory.name());
        }
      } else if (Objects.equals(latestRoyaltyRunNumberFromBQ, lastRoyaltyRunNumberFromDB)) {
        log.info(
            "For {} territory, latest royalty run number from Bigquery : {} and last sync royalty run number from DB : {} is same",
            territory.name(),
            latestRoyaltyRunNumberFromBQ,
            lastRoyaltyRunNumberFromDB);
        // case where lastRoyaltyRunNumberFromBQ is equal to lastRoyaltyRunNumberFromDB
        // In this case, we have to check
        // If sync finished date is null or empty
        // If yes, then we have to sync the master data for that particular territory
        String syncFinishedAt = masterDataSync.getSyncFinishedAt();
        boolean shouldRoyaltyRunHappen =
            calculateDaysDifferenceAndCheckSyncFinishedDate(syncStartedAt, syncFinishedAt);
        if (shouldRoyaltyRunHappen) {
          log.info(
              "Retrying sync for territory: {} due to incomplete previous run", territory.name());
          return latestRoyaltyRunNumberFromBQ;
        } else {
          log.info("No new recording royalty run identified for territory: {}.", territory.name());
        }
      }
      return null;
    } catch (InterruptedException e) {
      log.error(
          "Error checking publishing royalty run for territory: {}: {}",
          territory.name(),
          e.getMessage(),
          e);
      return null;
    }
  }
}

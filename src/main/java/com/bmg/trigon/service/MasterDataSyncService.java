package com.bmg.trigon.service;

import com.bmg.trigon.common.enums.Territory;
import com.bmg.trigon.common.util.ApplicationConstantsShared;
import com.bmg.trigon.dto.SAPMDResponse;
import com.bmg.trigon.exception.CustomException;
import com.bmg.trigon.repository.WorkbenchBatchDataRepository;
import com.bmg.trigon.util.ApplicationConstants;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MasterDataSyncService {

  private final FTPWatcherService ftpWatcherService;

  private final WorkbenchBatchDataRepository workbenchBatchDataRepository;

  public MasterDataSyncService(
      FTPWatcherService ftpWatcherService,
      WorkbenchBatchDataRepository workbenchBatchDataRepository) {
    this.ftpWatcherService = ftpWatcherService;
    this.workbenchBatchDataRepository = workbenchBatchDataRepository;
  }

  public Map<String, Object> refreshMasterData(List<String> vendorNumbers) {
    Map<String, Object> result = new HashMap<>();

    try {
      log.info("Processing vendor numbers from API: {}", vendorNumbers);

      List<Long> vendorIds = vendorNumbers.stream().map(Long::valueOf).distinct().toList();

      Map<String, Set<Long>> vendorProcessingResult = processVendorsWithoutFtpFilePath(vendorIds);

      result.put(
          ApplicationConstants.FAILED_VENDORS,
          vendorProcessingResult.get(ApplicationConstants.FAILED_VENDORS).stream()
              .map(String::valueOf) // Convert Long to String
              .collect(Collectors.toList()) // Collect to List<String>
          );

      result.put(
          ApplicationConstants.SUCCESSFUL_VENDORS,
          vendorProcessingResult.get(ApplicationConstants.SUCCESSFUL_VENDORS).stream()
              .map(String::valueOf) // Convert Long to String
              .collect(Collectors.toList()) // Collect to List<String>
          );

    } catch (Exception e) {
      log.error("Error occurred while processing vendors from API: {}", vendorNumbers, e);
      result.put(ApplicationConstants.FAILED_VENDORS, List.of()); // Empty list for failed vendors
      result.put(
          ApplicationConstants.SUCCESSFUL_VENDORS, List.of()); // Empty list for successful vendors
    }
    return result;
  }

  public void processAllVendors(Territory territory) {
    int pageSize = 100;
    int pageNumber = 0;

    PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
    Page<String> vendorNumbersPage;

    do {
      vendorNumbersPage =
          workbenchBatchDataRepository.findByTerritory(territory.name(), pageRequest);
      log.info(
          "Processing page: {}, size: {}, currentPage elements: {}, totalElements: {}",
          pageNumber,
          pageSize,
          vendorNumbersPage.getNumberOfElements(),
          vendorNumbersPage.getTotalElements());
      pageNumber++;
      if (vendorNumbersPage.hasContent()) {
        List<Long> vendorNumbers =
            vendorNumbersPage.getContent().stream()
                .map(String::trim)
                .map(this::convertToLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!vendorNumbers.isEmpty()) {
          processVendorsWithoutFtpFilePath(vendorNumbers);
        }
      }

      pageRequest = PageRequest.of(pageNumber, pageSize);
    } while (vendorNumbersPage.hasNext());
  }

  public Long convertToLong(String vendorNumber) {
    try {
      return Long.parseLong(vendorNumber);
    } catch (NumberFormatException e) {
      // Handle invalid format or log the error
      log.info("Failed to convert vendorId '{}' to Long. Invalid format.", vendorNumber);
      return null;
    }
  }

  public Map<String, Set<Long>> processVendorsWithoutFtpFilePath(List<Long> vendorNumbers) {

    Set<Long> successVendors = Collections.synchronizedSet(new HashSet<>());
    Set<SAPMDResponse> sapMDResponses = Collections.synchronizedSet(new HashSet<>());

    CompletableFuture.runAsync(
            () -> {
              log.info("Processing vendors: {}", vendorNumbers);

              for (Long vendorNumber : vendorNumbers) {
                SAPMDResponse sapMDResponse = new SAPMDResponse();
                try {
                  ftpWatcherService.processVendor(
                      vendorNumber, null, successVendors, sapMDResponse);
                } catch (CustomException e) {
                  log.error("Error while invoking the apis for vendor {}", vendorNumber, e);
                  sapMDResponse =
                      SAPMDResponse.builder()
                          .vendorNumber(vendorNumber)
                          .status(ApplicationConstantsShared.FAIL)
                          .apiResults(Map.of(e.getApiUrl(), e.getErrorMessage()))
                          .build();
                } finally {
                  if (ApplicationConstantsShared.FAIL.equals(sapMDResponse.getStatus())) {
                    sapMDResponses.add(sapMDResponse);
                  } else {
                    successVendors.add(vendorNumber);
                  }
                }
              }
            })
        .join();

    ftpWatcherService.handlePostProcessing(null, sapMDResponses, successVendors);

    // Return both failed and successful vendors as a Map
    Map<String, Set<Long>> result = new HashMap<>();
    result.put(
        ApplicationConstants.FAILED_VENDORS,
        sapMDResponses.stream().map(SAPMDResponse::getVendorNumber).collect(Collectors.toSet()));
    result.put(ApplicationConstants.SUCCESSFUL_VENDORS, successVendors);

    return result;
  }
}

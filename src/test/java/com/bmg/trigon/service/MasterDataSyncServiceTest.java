package com.bmg.trigon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bmg.trigon.common.enums.Territory;
import com.bmg.trigon.dto.SAPMDResponse;
import com.bmg.trigon.exception.CustomException;
import com.bmg.trigon.repository.WorkbenchBatchDataRepository;
import com.bmg.trigon.util.ApplicationConstants;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class MasterDataSyncServiceTest {

  @Mock FTPWatcherService ftpWatcherService;

  @Mock WorkbenchBatchDataRepository workbenchBatchDataRepository;

  @InjectMocks MasterDataSyncService masterDataSyncService;

  /**
   * Test for the refreshMasterData method Scenario: Successful processing of vendors from the API
   */
  @Test
  void testRefreshMasterDataSuccess() {
    List<String> vendorNumbers = Arrays.asList("1001", "1002", "1003");
    List<Long> vendorIds = Arrays.asList(1001L, 1002L, 1003L);
    Page<Long> vendorIdsPage = new PageImpl<>(vendorIds);

    Map<String, Set<Long>> vendorProcessingResult = new HashMap<>();
    vendorProcessingResult.put(ApplicationConstants.FAILED_VENDORS, new HashSet<>());
    vendorProcessingResult.put(ApplicationConstants.SUCCESSFUL_VENDORS, new HashSet<>(vendorIds));

    doNothing().when(ftpWatcherService).handlePostProcessing(any(), anySet(), anySet());

    Map<String, Object> result = masterDataSyncService.refreshMasterData(vendorNumbers);

    assertNotNull(result);
    assertTrue(result.containsKey(ApplicationConstants.FAILED_VENDORS));
    assertTrue(result.containsKey(ApplicationConstants.SUCCESSFUL_VENDORS));
    assertEquals(0, ((List<?>) result.get(ApplicationConstants.FAILED_VENDORS)).size());
    assertEquals(3, ((List<?>) result.get(ApplicationConstants.SUCCESSFUL_VENDORS)).size());
  }

  /**
   * Test for the refreshMasterData method Scenario: Exception thrown during processing of vendors
   * from API
   */
  @Test
  void testRefreshMasterDataWithException() {
    List<String> vendorNumbers = Arrays.asList("1001", "1002", "1003");

    doThrow(new RuntimeException("Test Exception"))
        .when(ftpWatcherService)
        .processVendor(anyLong(), any(), anySet(), any());

    Map<String, Object> result = masterDataSyncService.refreshMasterData(vendorNumbers);

    assertNotNull(result);
    assertTrue(result.containsKey(ApplicationConstants.FAILED_VENDORS));
    assertTrue(result.containsKey(ApplicationConstants.SUCCESSFUL_VENDORS));
    assertEquals(0, ((List<?>) result.get(ApplicationConstants.FAILED_VENDORS)).size());
    assertEquals(0, ((List<?>) result.get(ApplicationConstants.SUCCESSFUL_VENDORS)).size());
  }

  /**
   * Test for the processVendorsWithoutFtpFilePath method Scenario: Successful processing of vendor
   * numbers with no errors
   */
  @Test
  void testProcessVendorsWithoutFtpFilePathSuccess() {
    List<Long> vendorNumbers = Arrays.asList(1001L, 1002L, 1003L);
    Set<Long> successfulVendors = new HashSet<>(vendorNumbers);
    Set<SAPMDResponse> sapMDResponses = new HashSet<>();

    doNothing().when(ftpWatcherService).processVendor(anyLong(), any(), anySet(), any());
    doNothing().when(ftpWatcherService).handlePostProcessing(any(), anySet(), anySet());

    Map<String, Set<Long>> result =
        masterDataSyncService.processVendorsWithoutFtpFilePath(vendorNumbers);

    assertNotNull(result);
    assertTrue(result.containsKey(ApplicationConstants.FAILED_VENDORS));
    assertTrue(result.containsKey(ApplicationConstants.SUCCESSFUL_VENDORS));
    assertEquals(0, result.get(ApplicationConstants.FAILED_VENDORS).size());
    assertEquals(3, result.get(ApplicationConstants.SUCCESSFUL_VENDORS).size());
  }

  /**
   * Test for the processVendorsWithoutFtpFilePath method Scenario: CustomException is thrown during
   * vendor processing
   */
  @Test
  void testProcessVendorsWithoutFtpFilePathWithCustomException() throws CustomException {
    List<Long> vendorNumbers = Arrays.asList(1001L, 1002L, 1003L);
    Set<Long> successfulVendors = new HashSet<>();
    Set<SAPMDResponse> sapMDResponses = new HashSet<>();

    CustomException exception =
        new CustomException("API Error", HttpStatus.INTERNAL_SERVER_ERROR.value(), "API failure");

    doThrow(exception).when(ftpWatcherService).processVendor(anyLong(), any(), anySet(), any());
    doNothing().when(ftpWatcherService).handlePostProcessing(any(), anySet(), anySet());

    Map<String, Set<Long>> result =
        masterDataSyncService.processVendorsWithoutFtpFilePath(vendorNumbers);

    assertNotNull(result);
    assertTrue(result.containsKey(ApplicationConstants.FAILED_VENDORS));
    assertTrue(result.containsKey(ApplicationConstants.SUCCESSFUL_VENDORS));
    assertEquals(3, result.get(ApplicationConstants.FAILED_VENDORS).size());
    assertEquals(0, result.get(ApplicationConstants.SUCCESSFUL_VENDORS).size());
  }

  /** Test for the processAllVendors method. Scenario: Vendor numbers are successfully processed. */
  @Test
  void processAllVendors_shouldProcessVendorsSuccessfully() {
    Territory territory = Territory.UK;
    List<String> vendorNumbers = List.of("1001", "1002");
    Page<String> vendorNumbersPage = new PageImpl<>(vendorNumbers);

    when(workbenchBatchDataRepository.findByTerritory(any(), any(PageRequest.class)))
        .thenReturn(vendorNumbersPage);

    masterDataSyncService.processAllVendors(territory);

    verify(workbenchBatchDataRepository, times(1)).findByTerritory(any(), any(PageRequest.class));
  }

  /**
   * Test for the processAllVendors method. Scenario: No vendor numbers are found, and the method
   * handles the empty list correctly.
   */
  @Test
  void processAllVendors_shouldHandleEmptyVendorList() {
    Territory territory = Territory.BGUK;
    Page<String> emptyPage = new PageImpl<>(Collections.emptyList());

    when(workbenchBatchDataRepository.findByTerritory(any(), any(PageRequest.class)))
        .thenReturn(emptyPage);

    masterDataSyncService.processAllVendors(territory);

    verify(workbenchBatchDataRepository, times(1)).findByTerritory(any(), any(PageRequest.class));
  }

  /**
   * Test for the processAllVendors method. Scenario: Invalid vendor IDs are present, and the method
   * skips them during processing.
   */
  @Test
  void processAllVendors_shouldSkipInvalidVendorIds() {
    Territory territory = Territory.GE;
    List<String> vendorNumbers = List.of("1001", "invalidNumber");
    Page<String> vendorNumbersPage = new PageImpl<>(vendorNumbers);

    when(workbenchBatchDataRepository.findByTerritory(any(), any(PageRequest.class)))
        .thenReturn(vendorNumbersPage);

    masterDataSyncService.processAllVendors(territory);

    verify(workbenchBatchDataRepository, times(1)).findByTerritory(any(), any(PageRequest.class));
  }
}

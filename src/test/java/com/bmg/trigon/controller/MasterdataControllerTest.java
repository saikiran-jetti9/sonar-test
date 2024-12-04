package com.bmg.trigon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bmg.trigon.common.dto.TrigonResponse;
import com.bmg.trigon.service.MasterDataSyncService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MasterdataControllerTest {

  @Mock private MasterDataSyncService masterDataSyncService;

  @InjectMocks private MasterdataController masterdataController;

  /**
   * Test case to verify successful refresh of master data. This simulates a scenario where a list
   * of vendor numbers is provided, and the service returns a successful response.
   */
  @Test
  public void testRefreshMasterData_Success() {
    List<String> vendorNumbers = Arrays.asList("3100313", "3102831");
    Map<String, Object> expectedResponse = new HashMap<>();
    expectedResponse.put("status", "success");

    when(masterDataSyncService.refreshMasterData(vendorNumbers)).thenReturn(expectedResponse);

    TrigonResponse response = masterdataController.refreshMasterData(vendorNumbers);

    assertEquals(expectedResponse, response.getData());
    assertEquals(new HashMap<>(), response.getMeta());
  }

  /**
   * Test case to verify behavior when an empty list of vendor numbers is provided. This simulates a
   * scenario where no vendor numbers are available for refreshing, and ensures the service can
   * handle such input gracefully.
   */
  @Test
  public void testRefreshMasterData_EmptyVendorNumbers() {
    List<String> vendorNumbers = Arrays.asList(); // Empty list
    Map<String, Object> expectedResponse = new HashMap<>();
    expectedResponse.put("status", "success");

    when(masterDataSyncService.refreshMasterData(vendorNumbers)).thenReturn(expectedResponse);

    TrigonResponse response = masterdataController.refreshMasterData(vendorNumbers);

    assertEquals(expectedResponse, response.getData());
    assertEquals(new HashMap<>(), response.getMeta());
  }
}

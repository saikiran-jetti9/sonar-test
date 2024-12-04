package com.bmg.trigon.controller;

import com.bmg.trigon.common.dto.TrigonResponse;
import com.bmg.trigon.service.MasterDataSyncService;
import com.bmg.trigon.util.ApplicationConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MasterdataController extends BaseController {

  private final MasterDataSyncService masterDataSyncService;

  public MasterdataController(MasterDataSyncService masterDataSyncService) {
    this.masterDataSyncService = masterDataSyncService;
  }

  /**
   * Endpoint to refresh master data based on vendor numbers.
   *
   * @param vendorNumbers a list of vendor numbers to be refreshed.
   * @return a TrigonResponse containing the response data.
   */
  @PostMapping(value = ApplicationConstants.REFRESH_MASTER_DATA)
  public TrigonResponse refreshMasterData(@RequestBody List<String> vendorNumbers) {
    Map<String, Object> responseMap = masterDataSyncService.refreshMasterData(vendorNumbers);
    return TrigonResponse.builder().meta(new HashMap<>()).data(responseMap).build();
  }
}

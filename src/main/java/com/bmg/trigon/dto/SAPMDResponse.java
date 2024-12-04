package com.bmg.trigon.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SAPMDResponse {
  private Long vendorNumber;
  private String status;
  // here api URL is stored as Key and Exception is stored as value
  @Builder.Default private Map<String, String> apiResults = new HashMap<>();
}
